import json
import socket
import argparse
import time
import subprocess
import os
import signal
import traceback

UDP_IP = "0.0.0.0"  # All interfaces
UDP_PORT = 12346
STREAM_DST_IP = '192.168.2.5'
# STREAM_DST_IP = '192.168.1.181'
# STREAM_DST_IP = '0.0.0.0'

PWM_YAW_PIN = 18    # GPIO18 (PWM0)
PWM_PITCH_PIN = 12  # GPIO19 (PWM1)
PWM_STEER_PIN = 13  # GPIO12 (PWM0)
PWM_LONG_PIN = 19   # GPIO13 (PWM1)
PWM_GIMB_MODE = 23
PWM_GIMB_SENS = 24

GIMB_MODE_VAL = 1000
GIMB_SENS_VAL = 1000

# PWM options
PWM_FREQUENCY = 50  
PWM_MIN = 500      
PWM_MAX = 2500    
PWM_MIN_LONG = 1200      
PWM_MAX_LONG = 1700  
NO_DATA_TIMEOUT = 0.25

class Controller:

    def __init__(self, dry_run: bool):
        self._dry_run = dry_run
        if self._dry_run:
            self._pi_handler = EmptyHandler()
        else:
            self._pi_handler = PiHandler()
        self._last_stream_cmd = ""
        self._stream_cmd_hash = ""
        self.stream_process = None
        
        if not self._pi_handler.connected():
            print("Error: could not connect to pigpio. Make sure pigpiod is running.")
            exit(1)
        for pin in [PWM_YAW_PIN, PWM_PITCH_PIN, PWM_STEER_PIN, PWM_LONG_PIN]:
            self._pi_handler.do(lambda pi: pi.set_PWM_frequency(pin, PWM_FREQUENCY))
        
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.bind((UDP_IP, UDP_PORT))
        self.sock.settimeout(0.25)
        print(f"Listening UDP on {UDP_IP}:{UDP_PORT}")

    def _get_default_stream_command(self) -> str:
        if self._dry_run:
            return (
                    'python3 fake_video_stream.py |'
                    'gst-launch-1.0 --verbose '
                    '    fdsrc ! '
                    '    h264parse ! '
                    '    rtph264pay ! '
                    '    udpsink '
                    f'    host={STREAM_DST_IP} '
                    '    port=12345 '
            )

        return (
                'raspivid -pf baseline -awb cloud -fl -g 1 -w 320 -h 240 '
                '--nopreview -fps 30/1 -t 0 -o - | '
                'gst-launch-1.0 fdsrc ! h264parse ! rtph264pay ! '
                f'udpsink host={STREAM_DST_IP} port=12345'
        )
    
    def start_stream(self, command=None):
        if command is None:
            command = self._get_default_stream_command()
        
        self.stop_stream()
        print(f'===> Starting stream with command: {command}')
        python_script_path = os.path.dirname(__file__)
        with open("/tmp/fpv_controller_stream.log", "a") as log_file:
            self.stream_process = subprocess.Popen(
                command, shell=True, stdout=log_file, stderr=log_file,
                cwd=python_script_path, preexec_fn=os.setpgrp
            )

    def stop_stream(self):
        if self.stream_process:
            print("===> Stopping stream")
            try:
                os.killpg(self.stream_process.pid, signal.SIGTERM)  # Kill the process group
            except ProcessLookupError:
                pass  # Process may have already exited

            self.stream_process.wait()  # Ensure process is fully terminated
            self.stream_process = None

    def stop_servo_pulse(self):
        for pin in [PWM_YAW_PIN, PWM_PITCH_PIN, PWM_STEER_PIN, PWM_LONG_PIN, PWM_GIMB_MODE, PWM_GIMB_SENS]:
            self._pi_handler.do(lambda pi: pi.set_servo_pulsewidth(pin, 0));

    def scale_pwm(self, value):
        return int(PWM_MIN + (value + 1) * 0.5 * (PWM_MAX - PWM_MIN))

    def handle_data(self, data):
        msg = None
        try:
            msg = json.loads(data.decode("utf-8"))
        except UnicodeDecodeError as e:
            print(f"Failed to decode data as UTF-8: {e}")
            traceback.print_exc()
            return
        except json.JSONDecodeError as e:
            print(f"Failed to parse JSON: {e}")
            traceback.print_exc()
            return        
        yaw = msg.get("yaw", 0)       # -1..1
        pitch = msg.get("pitch", 0)   # -1..1
        steer = msg.get("steer", 0)   # -1..1
        long = msg.get("long", 0)    # -1..1
        stream_cmd = msg.get("stream_cmd", "") 
        stream_cmd_hash = msg.get("stream_cmd_hash", "") 

        if not all(-1 <= v <= 1 for v in [yaw, pitch, steer, long]):
            print(f"Incorrect data: {msg}")
            return

        pwm_yaw = self.scale_pwm(yaw)
        pwm_pitch = self.scale_pwm(pitch)
        pwm_steer = self.scale_pwm(-steer)
        pwm_long = int(PWM_MIN_LONG + (long + 1) * 0.5 * (PWM_MAX_LONG - PWM_MIN_LONG))

        self._pi_handler.do(lambda pi: pi.set_servo_pulsewidth(PWM_YAW_PIN, pwm_yaw))
        self._pi_handler.do(lambda pi: pi.set_servo_pulsewidth(PWM_PITCH_PIN, pwm_pitch))
        self._pi_handler.do(lambda pi: pi.set_servo_pulsewidth(PWM_STEER_PIN, pwm_steer))
        self._pi_handler.do(lambda pi: pi.set_servo_pulsewidth(PWM_LONG_PIN, pwm_long))
        self._pi_handler.do(lambda pi: pi.set_servo_pulsewidth(PWM_GIMB_MODE, GIMB_MODE_VAL))
        self._pi_handler.do(lambda pi: pi.set_servo_pulsewidth(PWM_GIMB_SENS, GIMB_SENS_VAL))
        print(f"Received: yaw={yaw}, pitch={pitch}, steer={steer}, long={long} => PWM: {pwm_yaw}, {pwm_pitch}, {pwm_steer}, {pwm_long}")

        self._try_update_stream(stream_cmd, stream_cmd_hash)
            

    def _try_update_stream(self, cmd: str, hash: str) -> None:
        if not cmd or len(cmd) == 0:
            return
        
        if hash == self._stream_cmd_hash and cmd == self._last_stream_cmd:
            return
        
        self._stream_cmd_hash = hash
        self._last_stream_cmd = cmd
        self.start_stream(cmd)

    def start(self):
        last_received_time = time.time()
        self.start_stream()

        try:
            while True:
                try:
                    data, addr = self.sock.recvfrom(1024)
                    last_received_time = time.time()
                    self.handle_data(data)
                except socket.timeout:
                    if time.time() - last_received_time > NO_DATA_TIMEOUT:
                        print("No data received for 250ms, stopping servo pulses.")
                        self.stop_servo_pulse()
                        last_received_time = time.time()
                except Exception as e:
                    print(f"An unknown error occurred: {e}")
                    traceback.print_exc()
        except KeyboardInterrupt:
            print("\nEnding...")
            self.stop_stream()
            self.sock.close()
            self.stop_servo_pulse()
            self._pi_handler.do(lambda pi: pi.stop())
            print("PWM cleared and pigpio connection closed.")

class PiHandler:
    def __init__(self):
        import pigpio
        self._pi = pigpio.pi()

    def connected(self) -> bool:
        return self._pi.connected

    def do(self, action):
        action(self._pi)


class EmptyHandler:
    def connected(self) -> bool:
        return True

    def do(self, action):
        pass
    


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true", help="Run script without controlling pigpio")
    args = parser.parse_args()
    controller = Controller(args.dry_run)
    controller.start()

if __name__ == "__main__":
    main()

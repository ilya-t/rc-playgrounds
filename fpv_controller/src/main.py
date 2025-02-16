import json
import socket
import argparse
import time
import subprocess

UDP_IP = "0.0.0.0"  # All interfaces
UDP_PORT = 12346

PWM_YAW_PIN = 18    # GPIO18 (PWM0)
PWM_PITCH_PIN = 12  # GPIO19 (PWM1)
PWM_STEER_PIN = 13  # GPIO12 (PWM0)
PWM_LONG_PIN = 19   # GPIO13 (PWM1)

# PWM options
PWM_FREQUENCY = 50  
PWM_MIN = 500      
PWM_MAX = 2500    

PWM_MIN_LONG = 1200      
PWM_MAX_LONG = 1700  

NO_DATA_TIMEOUT = 0.25

# Launching the stream in the background
def start_stream():
    print('===> Starting stream')
    command = (
        'raspivid -pf baseline -awb cloud -fl -g 1 -w 320 -h 240 '
        '--nopreview -fps 30/1 -t 0 -o - | '
        'gst-launch-1.0 fdsrc ! h264parse ! rtph264pay ! '
        'udpsink host=192.168.2.5 port=12345 >> /tmp/fpv_controller_stream.log &'
    )
    
    subprocess.Popen(command, shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

def stop_servo_pulse():
    for pin in [PWM_YAW_PIN, PWM_PITCH_PIN, PWM_STEER_PIN, PWM_LONG_PIN]:
        pi.set_servo_pulsewidth(pin, 0)

def scale_pwm(value):
    return int(PWM_MIN + (value + 1) * 0.5 * (PWM_MAX - PWM_MIN))

def handle_data(data):
    try:
        msg = json.loads(data.decode("utf-8"))
        yaw = msg.get("yaw", 0)       # -1..1
        pitch = msg.get("pitch", 0)   # -1..1
        steer = msg.get("steer", 0)   # -1..1
        long = msg.get("long", 0) + 0.18    # -1..1

        # Check data
        if not all(-1 <= v <= 1 for v in [yaw, pitch, steer, long]):
            print(f"Incorrect data: {msg}")
            return

        # Translating to PWM
        pwm_yaw = scale_pwm(yaw)
        pwm_pitch = scale_pwm(pitch)
        pwm_steer = scale_pwm(steer)
        pwm_long = int(PWM_MIN_LONG + (long + 1) * 0.5 * (PWM_MAX_LONG - PWM_MIN_LONG))

        if dry_run:
            print(f"Dry-run: Received yaw={yaw}, pitch={pitch}, steer={steer}, long={long} => PWM: {pwm_yaw}, {pwm_pitch}, {pwm_steer}, {pwm_long}")
        else:
            pi.set_servo_pulsewidth(PWM_YAW_PIN, pwm_yaw)
            pi.set_servo_pulsewidth(PWM_PITCH_PIN, pwm_pitch)
            pi.set_servo_pulsewidth(PWM_STEER_PIN, pwm_steer)
            pi.set_servo_pulsewidth(PWM_LONG_PIN, pwm_long)
            print(f"Received: yaw={yaw}, pitch={pitch}, steer={steer}, long={long} => PWM: {pwm_yaw}, {pwm_pitch}, {pwm_steer}, {pwm_long}")
    except json.JSONDecodeError:
        print("JSON parsing error:", data.decode("utf-8"))    

# Argument parsing
parser = argparse.ArgumentParser()
parser.add_argument("--dry-run", action="store_true", help="Run script without controlling pigpio")
args = parser.parse_args()

dry_run = args.dry_run

if not dry_run:
    import pigpio
    pi = pigpio.pi()
    if not pi.connected:
        print("Error: could not connect to pigpio. Make sure pigpiod is running.")
        exit(1)
    # PWM pins setup
    for pin in [PWM_YAW_PIN, PWM_PITCH_PIN, PWM_STEER_PIN, PWM_LONG_PIN]:
        pi.set_PWM_frequency(pin, PWM_FREQUENCY)
else:
    print("Running in --dry-run mode. No pigpio interaction.")

# Start the stream
start_stream()

# UDP socket creation
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((UDP_IP, UDP_PORT))
sock.settimeout(0.25)  # Set timeout to 250ms

print(f"Listening UDP on {UDP_IP}:{UDP_PORT}")

last_received_time = time.time()

try:
    while True:
        try:
            data, addr = sock.recvfrom(1024)  # Receiving data (max. 1024 bytes)
            last_received_time = time.time()
            handle_data(data)
        except socket.timeout:
            if time.time() - last_received_time > NO_DATA_TIMEOUT:
                print("No data received for 250ms, stopping servo pulses.")
                stop_servo_pulse()
                last_received_time = time.time()  # Prevent repeated stops

except KeyboardInterrupt:
    print("\nEnding...")
    sock.close()
    if not dry_run:
        stop_servo_pulse()
        pi.stop()
        print("PWM cleared and pigpio connection closed.")

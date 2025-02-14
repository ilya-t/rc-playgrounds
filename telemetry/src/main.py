import json
import socket
import pigpio

UDP_IP = "0.0.0.0"  # All interfaces
UDP_PORT = 12346

PWM_YAW_PIN = 18    # GPIO18 (PWM0)
PWM_PITCH_PIN = 19  # GPIO19 (PWM1)
PWM_STEER_PIN = 12  # GPIO12 (PWM0)
PWM_LONG_PIN = 13   # GPIO13 (PWM1)

# PWM options
PWM_FREQUENCY = 50  
PWM_MIN = 500      
PWM_MAX = 2500    

PWM_MIN_LONG = 1200      
PWM_MAX_LONG = 1700  

def scale_pwm(value):
    return int(PWM_MIN + (value + 1) * 0.5 * (PWM_MAX - PWM_MIN))

pi = pigpio.pi()
if not pi.connected:
    print("Error: could not connect to pigpio. Make sure pigpiod is run.")
    exit(1)

# PWM pins setup
for pin in [PWM_YAW_PIN, PWM_PITCH_PIN, PWM_STEER_PIN, PWM_LONG_PIN]:
    pi.set_PWM_frequency(pin, PWM_FREQUENCY)

# UDP socket creation
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((UDP_IP, UDP_PORT))

print(f"Listening UDP on {UDP_IP}:{UDP_PORT}")

try:
    while True:
        data, addr = sock.recvfrom(1024)  # Receiving data (max. 1024 байта)
        try:
            msg = json.loads(data.decode("utf-8"))
            yaw = msg.get("yaw", 0)       # -1..1
            pitch = msg.get("pitch", 0)   # -1..1
            steer = msg.get("steer", 0)   # -1..1
            long = msg.get("long", 0) + 0.18    # -1..1

            # Check data
            if not all(-1 <= v <= 1 for v in [yaw, pitch, steer, long]):
                print(f"Incorrect data: {msg}")
                continue

            # Translating to PWM
            pwm_yaw = scale_pwm(yaw)
            pwm_pitch = scale_pwm(pitch)
            pwm_steer = scale_pwm(steer)
            pwm_long = int(PWM_MIN_LONG + (long + 1) * 0.5 * (PWM_MAX_LONG - PWM_MIN_LONG))

            pi.set_servo_pulsewidth(PWM_YAW_PIN, pwm_yaw)
            pi.set_servo_pulsewidth(PWM_PITCH_PIN, pwm_pitch)
            pi.set_servo_pulsewidth(PWM_STEER_PIN, pwm_steer)
            pi.set_servo_pulsewidth(PWM_LONG_PIN, pwm_long)
            
            print("pwm_long: ", pwm_long)

            print(f"Received: yaw={yaw}, pitch={pitch}, steer={steer}, long={long} => PWM: {pwm_yaw}, {pwm_pitch}, {pwm_steer}, {pwm_long}")

        except json.JSONDecodeError:
            print("JSON parsing error:", data.decode("utf-8"))

except KeyboardInterrupt:
    print("\nEnding...")
    sock.close()
    for pin in [PWM_YAW_PIN, PWM_PITCH_PIN, PWM_STEER_PIN, PWM_LONG_PIN]:
        pi.set_servo_pulsewidth(pin, 0)
    pi.stop()
    print("PWM cleared and pigpio connection closed.")

# RC MicroView Controller v2 — Android and SparkFun MicroView

The Android app transmits six RC PWM channels to a MicroView over an OTG
USB-to-TTL-UART cable. The MicroView controls a motor ESC, steering servo,
2-axis gimbal, gimbal mode, and movement smoothness. No ARM/DISARM UI.

## Outputs

1. CH1 D2 motor ESC (1500 us neutral for reversible ESC)
2. CH2 D3 steering
3. CH3 D5 yaw
4. CH4 D6 pitch
5. CH5 A0 gimbal mode (1100/1500/1900 us placeholders)
6. CH6 A1 smoothness (1000..2000 us)

MicroView UART D0/RX receives **adapter TX** and D1/TX sends to **adapter RX**.
GND must be common. Use a correctly leveled **5 V-compatible UART** for the
MicroView (which is a 5 V ATmega328P device). USB cable must support OTG.
Do **not** connect 5 V TX directly to a 3.3 V-only UART RX. Servos/ESC have
separate suitable power supply; do not supply them from MicroView pins.

## Quick build

- Android: `cd android && gradle :app:assembleDebug` with JDK 17, Android SDK 35
  and Gradle 8.9. APK: `android/app/build/outputs/apk/debug/app-debug.apk`.
- MicroView: flash `firmware/rc_microview/rc_microview.ino` using the SparkFun
  MicroView library and Arduino Servo library.
- CI: GitHub Actions produces the downloadable `RC-MicroView-debug-apk` artifact.

**Important:** app and firmware must both be protocol v2; old versions are
incompatible. See [PROTOCOL.md](PROTOCOL.md).

## Safety

No ARM step: throttle outputs are live when commands are received.
Release the joystick to center it. App loses focus/disconnect -> neutral.
Arduino hardware-side watchdog defaults to 350 ms without fresh commands.
Test with the drive wheels off the ground and set ESC neutral appropriately.
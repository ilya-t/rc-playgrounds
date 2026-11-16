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
- CI: GitHub Actions produces the downloadable `RC-MicroView-v2-debug-apk` artifact.

**Important:** app and firmware must both be protocol v2; old versions are
incompatible. See [PROTOCOL.md](PROTOCOL.md).

## Android trim and inversion (0.2.0)

- On the main landscape screen, the **TRIMS** strip shows all six channels.
  Each row uses `-`/`+` to shift the channel in **5 us** increments, up to
  **-200..+200 us**. Tap the displayed signed number (e.g. `+15`) to reset
  *that channel* to zero. Tap the **TRIMS** header button to collapse/expand
  the panel and gain more joystick space. Long-press **TRIMS** to reset all six.
- Tap **INVERT** in the header to open six independent inversion checkboxes.
  The settings dialog also has **Reset all**.
- The resulting PWM is `clamp(1500 + (inverted ? -(base-1500) : base-1500)
  + trim, 1000, 2000)`. The base comes from the joystick, gimbal mode or
  smoothness slider. Inversion of CH5 reverses the gimbal mode PWM order.
- Both trims and inversion settings are saved with Android `SharedPreferences`
  and restored on the next launch. They are applied on Android **before** the
  UART command is sent. **The MicroView firmware does not need to change.**

## Safety

No ARM step: throttle outputs are live when commands are received.
Release the joystick to center it. App loses focus/disconnect -> neutral.
Arduino hardware-side watchdog defaults to 350 ms without fresh commands.
Test with the drive wheels off the ground and set ESC neutral appropriately.

CAUTION: A nonzero **CH1 GAS trim** shifts the active ESC neutral away from
1500 us and can spin the wheels with the stick released. The app warns on the
first nonzero CH1 trim. On pause/disconnect the app sends an absolute 1500 us
for CH1 regardless of trim. A sudden broken USB link invokes MicroView's
firmware-side watchdog, which also sends its configured failsafe value.
# Build RC MicroView Android APK (v2)

On the `microview-android-v2` branch of `ilya-t/rc-playgrounds`, open
GitHub Actions → **MicroView Android APK (v2)**.

The push workflow builds `microview-controller/android` with JDK 17,
Gradle 8.9 and Android SDK 35 and publishes an installable **debug** APK
in `RC-MicroView-v2-debug-apk`. Download the artifact and install the
`app-debug.apk` inside it. This APK uses Android debug signing (suitable
for testing, not for Play Store distribution).

If running manually with Android SDK:

```bash
cd microview-controller/android
gradle :app:assembleDebug
```

**Flash firmware protocol v2** from `microview-controller/firmware` before
driving with this APK. Protocol v1 has a different number of command fields.
ARM/DISARM is removed entirely; motor control is immediate. Use an unloaded
motor or raised wheels for testing.
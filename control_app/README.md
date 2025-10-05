# Client remote control app

## Quick start
Download gstreamer for android at https://gstreamer.freedesktop.org/download/#android
At `local.properties` specify path to gstreamer:
```
gstAndroidRoot=/path/to/gstreamer-1.0-android-universal-1.24.12
```

If you don't have GStreamer available locally, set the following flag to build against a no-op stub module instead of the native integration:
```
gstreamer-stub=true
```

Now you're ready to build app:
```sh
./gradlew assembleDebug
```

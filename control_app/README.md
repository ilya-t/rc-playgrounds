# Client remote control app

## Quick start
Download gstreamer [**1.24.13**](https://gstreamer.freedesktop.org/data/pkg/android/1.24.13/) for android at https://gstreamer.freedesktop.org/download/#android
At `local.properties` specify path to gstreamer:
```
gstAndroidRoot=/path/to/gstreamer-1.0-android-universal-1.24.13
```

If you don't want to build with app with GStreamer 
set the following flag to build against a no-op stub module instead of the native integration:
```
gstreamer-stub=true
```

Now you're ready to build app:
```sh
./gradlew assembleDebug
```

import java.io.File
import java.util.Properties

val localProperties = File(rootDir, "local.properties")
val properties = Properties()
if (localProperties.exists()) {
    localProperties.inputStream().use { properties.load(it) }
}

val useGStreamerStub = properties.getProperty("gstreamer-stub")?.toBoolean() == true

include(":app")
include(":domain")

if (useGStreamerStub) {
    include(":gstreamer-stub")
    project(":gstreamer-stub").projectDir = File(rootDir, "gstreamer-stub")
} else {
    include(":gstreamer")
}

gradle.rootProject {
    extra["gstreamerModule"] = if (useGStreamerStub) ":gstreamer-stub" else ":gstreamer"
}

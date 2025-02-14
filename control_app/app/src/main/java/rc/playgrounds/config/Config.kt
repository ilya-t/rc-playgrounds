package rc.playgrounds.config

import android.net.Uri
import org.json.JSONObject
import rc.playgrounds.config.model.Telemetry

class Config(
    val rawJson: String,
) {
    private val json by lazy {
        runCatching {
            JSONObject(rawJson)
        }.getOrElse { JSONObject() }
    }

    val streamUrl: String?
        get() = runCatching {
                json.getJSONObject("stream").getString("url")
            }.getOrNull()

    val telemetry: Telemetry? by lazy {
        runCatching {
            val t = json.getJSONObject("telemetry")
            Telemetry(
                Uri.parse(t.getString("url")),
            )

        }.getOrNull()
    }
}
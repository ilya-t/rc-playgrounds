package rc.playgrounds.config

import org.json.JSONObject

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
}
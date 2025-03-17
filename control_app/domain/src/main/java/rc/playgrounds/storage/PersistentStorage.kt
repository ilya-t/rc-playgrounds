package rc.playgrounds.storage

import android.content.Context
import android.content.Context.MODE_PRIVATE

class PersistentStorage(
    context: Context,
) {
    val prefs = context.getSharedPreferences("main_storage", MODE_PRIVATE)

    fun readString(key: String): String? {
        return prefs.getString(key, null)
    }

    suspend fun writeString(key: String, value: String) {
        prefs.edit().putString(key, value).commit()
    }
}
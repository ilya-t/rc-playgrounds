package com.rc.playgrounds.storage

import android.content.Context
import android.content.SharedPreferences

class AndroidPersistentStorage(
    context: Context,
) : PersistentStorage {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("main_storage", Context.MODE_PRIVATE)

    override fun readString(key: String): String? {
        return prefs.getString(key, null)
    }

    override suspend fun writeString(key: String, value: String) {
        prefs.edit().putString(key, value).commit()
    }
}
package com.rc.playgrounds.storage

interface PersistentStorage {
    fun readString(key: String): String?
    suspend fun writeString(key: String, value: String)
}

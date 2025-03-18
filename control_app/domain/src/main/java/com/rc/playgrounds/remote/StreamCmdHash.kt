package com.rc.playgrounds.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class StreamCmdHash {
    private val _hash = MutableStateFlow(UUID.randomUUID().toString())
    val hash: Flow<String> = _hash

    fun invalidate() {
        _hash.value = UUID.randomUUID().toString()
    }
}

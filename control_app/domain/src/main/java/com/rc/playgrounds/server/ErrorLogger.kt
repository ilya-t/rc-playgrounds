package com.rc.playgrounds.server

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

data class Error(
    val service: String,
    val error: Throwable,
    val time: Long,
)

class ErrorLogger(
    private val scope: CoroutineScope,
) {
    private val reported = MutableSharedFlow<Error>(
        replay = BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val errors: SharedFlow<Error> = reported

    fun report(service: String, error: Throwable) {
        val happenedAt = System.currentTimeMillis()
        scope.launch(Dispatchers.IO) {
            Log.e(service, error.message.orEmpty(), error)
            reported.emit(Error(service, error, happenedAt))
        }
    }

    private companion object {
        const val BUFFER = 16
    }
}

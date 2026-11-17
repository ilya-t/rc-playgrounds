package com.rc.playgrounds.server

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ServerActiveConfigProvider {
    private val configs = MutableStateFlow(
        ServerStreamConfig(
            host = "192.168.1.37",
            port = 12345,
            bitrate = 2_000_000,
        )
    )
    val config: StateFlow<ServerStreamConfig> = configs

    fun updateTarget(host: String, port: Int) {
        configs.update { current ->
            if (current.host == host && current.port == port) {
                current
            } else {
                current.copy(host = host, port = port)
            }
        }
    }
}

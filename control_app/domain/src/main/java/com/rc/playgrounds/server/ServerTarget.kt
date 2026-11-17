package com.rc.playgrounds.server

data class ServerTarget(
    val host: String,
    val port: Int,
)

fun parseServerTarget(raw: String): ServerTarget? {
    val text = raw.trim()
    val separator = text.lastIndexOf(':')
    if (separator <= 0 || separator == text.lastIndex) {
        return null
    }
    val host = text.substring(0, separator).trim()
    val port = text.substring(separator + 1).trim().toIntOrNull() ?: return null
    if (host.isEmpty() || port !in 1..65535 || host.any(Char::isWhitespace)) {
        return null
    }
    return ServerTarget(host, port)
}

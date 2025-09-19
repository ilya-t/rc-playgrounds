package com.rc.playgrounds.config.env

internal fun String.applyEnv(env: Map<String, String>): String {
    val sb = StringBuilder(this.length)
    var i = 0
    while (i < this.length) {
        if (this[i] == '@' && i + 1 < this.length && this[i + 1] == '{') {
            val start = i + 2
            val end = this.indexOf('}', start)
            if (end != -1) {
                val key = this.substring(start, end)
                sb.append(env[key] ?: "@{$key}")
                i = end + 1
                continue
            }
        }
        sb.append(this[i])
        i++
    }
    return sb.toString()
}

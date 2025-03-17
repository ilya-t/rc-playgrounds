package com.rc.playgrounds.status

import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.time.measureTime
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class PingService(
    private val scope: CoroutineScope,
    private val host: String,
    private val intervalMilllis: Long = 1000L,
    private val timeout: Duration = 3000L.milliseconds,
) {
    private val _pingResult = MutableStateFlow<Result<Duration>?>(null)
    val pingResult: Flow<Result<Duration>> = _pingResult.filterNotNull()

    fun start(): Job {
        return scope.launch {
            while (isActive) {
                _pingResult.value = pingHost()
                delay(intervalMilllis)
            }
        }
    }

    private suspend fun pingHost(): Result<Duration> = withContext(Dispatchers.IO) {
        return@withContext try {
            val process = Runtime.getRuntime().exec("ping -c 1 $host")
            val processDuration = measureTime {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!process.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
                        process.destroy()
                        return@withContext Result.failure(
                            IOException("timed out(${timeout.inWholeSeconds}s)"))
                    }
                } else {
                    process.waitFor()
                }
            }
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            val extractedDuration = extractDuration(output)
                ?.takeIf { it < processDuration }
                ?: processDuration
            Result.success(extractedDuration)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: InterruptedException) {
            Result.failure(e)
        }
    }

    // PING 8.8.8.8 (8.8.8.8): 56 data bytes
    //64 bytes from 8.8.8.8: icmp_seq=0 ttl=57 time=102.519 ms
    //^C
    //--- 8.8.8.8 ping statistics ---
    //1 packets transmitted, 1 packets received, 0.0% packet loss
    //round-trip min/avg/max/stddev = 102.519/102.519/102.519/nan ms
    private fun extractDuration(pingOutput: String): Duration? {
        val regex = "time=([0-9]+(\\.[0-9]+)?) ms".toRegex()
        val match = regex.find(pingOutput)
        return match?.groupValues?.get(1)?.toDoubleOrNull()?.toDuration(DurationUnit.MILLISECONDS)
    }
}

package com.rc.playgrounds.config

import com.rc.playgrounds.storage.PersistentStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class ConfigRepositoryTest {
    private val testTimeout = 10.seconds
    private val storage = InMemoryStorage()
    private val scope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    private val underTest = ConfigRepository(
        storage,
        scope,
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test(timeout = 500L)
    fun defaults() {
        Assert.assertEquals("v.1", underTest.activeVersion.value.version)
        Assert.assertEquals(DEFAULT_CONFIG, underTest.activeVersion.value.rawConfig)
    }

    @Test
    fun `config updates`() = runTest(timeout = testTimeout) {
        val newVersion = ConfigVersion("v.2", MINIMAL_TEST_CONFIG)
        underTest.storeConfigOrThrow(newVersion)
        underTest.switchActive(newVersion.version)

        val newActiveVersion = underTest.activeVersion
            .first { it.version == newVersion.version }.rawConfig
        Assert.assertEquals(newVersion.rawConfig, newActiveVersion)
    }

    @Test
    fun `update active config`() = runTest(timeout = testTimeout) {
        val newVersion = underTest.activeVersion.value.copy(rawConfig = MINIMAL_TEST_CONFIG)
        underTest.storeConfigOrThrow(newVersion)

        val newActiveVersion = underTest.activeVersion
            .first { it.rawConfig == newVersion.rawConfig }
        Assert.assertEquals(newVersion, newActiveVersion)
    }

    private suspend fun ConfigRepository.storeConfigOrThrow(newVersion: ConfigVersion) {
        val result = this.storeConfig(newVersion)
        result.await().onFailure {
            throw AssertionError(it)
        }
    }

    @Test
    fun `data restored between sessions`() = runTest(timeout = testTimeout) {
        val newVersion = underTest.activeVersion.value.copy(rawConfig = MINIMAL_TEST_CONFIG)
        underTest.storeConfigOrThrow(newVersion)

        underTest.activeVersion.first { it.rawConfig == newVersion.rawConfig }

        val underTest2 = ConfigRepository(storage, scope)

        Assert.assertEquals(newVersion, underTest2.activeVersion.value)
    }

    @Test
    fun `data restored between sessions (multi-config)`() = runTest(timeout = testTimeout) {
        val v1 = underTest.activeVersion.value.copy(rawConfig = MINIMAL_TEST_CONFIG)
        val v4 = ConfigVersion(version = "v.4", rawConfig = MINIMAL_TEST_CONFIG_2)
        underTest.storeConfigOrThrow(v4)
        underTest.storeConfigOrThrow(v1)

        underTest.activeVersion.first { it.rawConfig == v1.rawConfig }

        val underTest2 = ConfigRepository(storage, scope)
        underTest2.switchActive(v4.version)

        underTest2.activeVersion.first { it.rawConfig == v4.rawConfig }
        Assert.assertEquals(v4, underTest2.activeVersion.value)
    }
}

class InMemoryStorage : PersistentStorage {
    private val data = mutableMapOf<String, String>()

    override fun readString(key: String): String? {
        return data[key]
    }

    override suspend fun writeString(key: String, value: String) {
        data[key] = value
    }

}

internal val MINIMAL_TEST_CONFIG_2 = """{
            "stream": {
                "local_cmd": "pwd",
                "remote_cmd": "ls",
                "quality_profiles": []
            },
            "control_server": {
                "address": "127.0.0.1", 
                "port": "7070"
            },
            "stream_target": {
                "address": "127.0.0.2", 
                "port": "7171"
            },
            "control_offsets": {
                "pitch": 0.0,
                "yaw": 0.1,
                "steer": 0.0,
                "long": 0.0
            }
        }""".trimIndent()

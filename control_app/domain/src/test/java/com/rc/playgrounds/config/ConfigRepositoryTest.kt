package com.rc.playgrounds.config

import com.rc.playgrounds.storage.PersistentStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigRepositoryTest {
    private val storage = InMemoryStorage()
    private val scope = CoroutineScope(Dispatchers.IO)
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

    @Test(timeout = 5000L)
    fun `config updates`() = runTest {
        val newVersion = ConfigVersion("v.2", "{}")
        underTest.storeConfig(newVersion)
        underTest.switchActive(newVersion.version)

        val newActiveVersion = underTest.activeVersion
            .first { it.version == newVersion.version }.rawConfig
        Assert.assertEquals(newVersion.rawConfig, newActiveVersion)
    }

    @Test(timeout = 5000L)
    fun `update active config`() = runTest {
        val newVersion = underTest.activeVersion.value.copy(rawConfig = "{}")
        underTest.storeConfig(newVersion)

        val newActiveVersion = underTest.activeVersion
            .first { it.rawConfig == newVersion.rawConfig }
        Assert.assertEquals(newVersion, newActiveVersion)
    }

    @Test
    fun `data restored between sessions`() = runTest {
        val newVersion = underTest.activeVersion.value.copy(rawConfig = "{}")
        underTest.storeConfig(newVersion)

        underTest.activeVersion.first { it.rawConfig == newVersion.rawConfig }

        val underTest2 = ConfigRepository(storage, scope)

        Assert.assertEquals(newVersion, underTest2.activeVersion.value)
    }

    @Test
    fun `data restored between sessions (multi-config)`() = runTest {
        val v1 = underTest.activeVersion.value.copy(rawConfig = "{}")
        val v4 = ConfigVersion(version = "v.4", rawConfig = "[]")
        underTest.storeConfig(v4)
        underTest.storeConfig(v1)

        underTest.activeVersion.first { it.rawConfig == v1.rawConfig }

        val underTest2 = ConfigRepository(storage, scope)
        println("+${underTest2}")
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

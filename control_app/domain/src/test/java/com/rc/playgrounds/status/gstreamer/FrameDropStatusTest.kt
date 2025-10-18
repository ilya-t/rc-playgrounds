package com.rc.playgrounds.status.gstreamer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FrameDropStatusTest {

    private val testScope = TestScope()
    private val streamerEvents = MutableSharedFlow<Event>()
    private val frameDropStatus = FrameDropStatus(streamerEvents, testScope)

    @Test
    fun `test frame drops per second`() = runTest {
        // Emit frame drop events
        streamerEvents.emit(Event.Message("Dropping frame due to QoS."))
        streamerEvents.emit(Event.Message("Dropping frame due to QoS."))

        // Wait for the state to be updated
        val frameDrops = frameDropStatus.frameDropsPerSecond.first()
        assertEquals(2, frameDrops)
    }

    @Test
    fun `test no frame drops`() = runTest {
        // Emit non-frame drop events
        streamerEvents.emit(Event.Message("Some other message"))

        // Wait for the state to be updated
        val frameDrops = frameDropStatus.frameDropsPerSecond.first()
        assertEquals(-1, frameDrops)
    }

    @Test
    @Ignore
    fun `test frame drops expire after one second`() = runTest {
        // Emit frame drop events
        streamerEvents.emit(Event.Message("Dropping frame due to QoS.", time = 0))
        streamerEvents.emit(Event.Message("Dropping frame due to QoS.", time = 1001))
        advanceTimeBy(100) // Move time forward by just over one second

        // Wait for the state to be updated
        val frameDrops = frameDropStatus.frameDropsPerSecond.first()
        assertEquals(1, frameDrops)
    }

    @Test
    @Ignore
    fun `test multiple frame drops during one second`() = runTest {
        // Emit frame drop events
        streamerEvents.emit(Event.Message("Dropping frame due to QoS.", time = 0))
        streamerEvents.emit(Event.Message("Dropping frame due to QoS.", time = 100))
        streamerEvents.emit(Event.Message("Dropping frame due to QoS.", time = 250))
        advanceTimeBy(100) // Move time forward by just over one second

        // Wait for the state to be updated
        val frameDrops = frameDropStatus.frameDropsPerSecond.first()
        assertEquals(3, frameDrops)
    }
}

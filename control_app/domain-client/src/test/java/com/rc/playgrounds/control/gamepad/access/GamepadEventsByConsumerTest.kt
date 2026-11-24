package com.rc.playgrounds.control.gamepad.access

import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [GamepadEventsByConsumer].
 */
class GamepadEventsByConsumerTest {

    private val timeout = 10.seconds

    private val scope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )

    private val buttonEvents = MutableSharedFlow<GamepadButtonPress>()

    private val gamepadEventStream = mock<GamepadEventStream> {
        on { buttonEvents } doReturn buttonEvents
    }

    private val underTest = GamepadEventsByConsumer(
        gamepadEventStream = gamepadEventStream,
        scope = scope,
    )

    private val consumerA = EventConsumer.MainView
    private val consumerB = EventConsumer.LockView
    private val press1 = GamepadButtonPress.A
    private val press2 = GamepadButtonPress.B
    private val press3 = GamepadButtonPress.Up

    @Test
    fun `smoke flow identity per consumer`() = runTest(timeout = timeout) {
        val flow1 = underTest.buttonEventsFor(consumerA)
        val flow2 = underTest.buttonEventsFor(consumerA)
        Assert.assertTrue(flow1 === flow2)
    }

    @Test
    fun `routes events only to first active consumer`() = runTest(timeout = timeout) {
        val flowA = underTest.buttonEventsFor(consumerA)
        val flowB = underTest.buttonEventsFor(consumerB)

        val eventsA = mutableListOf<GamepadButtonPress>()
        val eventsB = mutableListOf<GamepadButtonPress>()
        val lastEvent = MutableStateFlow<GamepadButtonPress?>(null)
        val jobA = scope.launch {
            println("collecting flowA")
            flowA.collect {
                println("flowA received '$it'")
                eventsA.add(it)
                lastEvent.value = it
            }
        }
        val jobB = scope.launch {
            println("collecting flowB")
            flowB.collect {
                print("flowB received '$it'")
                eventsB.add(it)
                lastEvent.value = it
            }
        }

        underTest.acquireFocus(consumerA)
        emit(press1)
        lastEvent.first { it == press1 }
        Assert.assertEquals(1, eventsA.size)
        Assert.assertEquals(0, eventsB.size)

        underTest.acquireFocus(consumerB)
        emit(press2)
        lastEvent.first { it == press2 }
        Assert.assertEquals(1, eventsA.size)
        Assert.assertEquals(1, eventsB.size)

        underTest.releaseFocus(consumerB)
        emit(press3)
        lastEvent.first { it == press3 }
        Assert.assertEquals(2, eventsA.size)
        Assert.assertEquals(1, eventsB.size)

        jobA.cancel()
        jobB.cancel()
    }

    @Test
    fun `release focus is idempotent`() = runTest(timeout = timeout) {
        val flowA = underTest.buttonEventsFor(consumerA)
        val chA = Channel<GamepadButtonPress>(capacity = Channel.UNLIMITED)
        val jobA = launch { flowA.collect { chA.send(it) } }

        underTest.acquireFocus(consumerA)
        underTest.releaseFocus(consumerA)
        underTest.releaseFocus(consumerA)

        emit(press1)
        Assert.assertTrue(chA.isEmpty)

        jobA.cancel()
        chA.close()
    }

    private suspend fun emit(press: GamepadButtonPress) = buttonEvents.emit(press)
}

package com.rc.playgrounds.control.gamepad.access

import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GamepadEventsByConsumer(
    private val gamepadEventStream: GamepadEventStream,
    private val scope: CoroutineScope,
) {
    private val eventConsumers = MutableStateFlow(emptyList<ConsumerState>())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = Dispatchers.Default.limitedParallelism(1)

    init {
        scope.launch {
            gamepadEventStream.buttonEvents.collect { event ->
                val target = eventConsumers.value.firstOrNull { it.active }
                target?.events?.emit(event)
            }
        }
    }

    suspend fun buttonEventsFor(consumer: EventConsumer): Flow<GamepadButtonPress> {
        return withContext(dispatcher) {
            getOrCreate(consumer).events
        }
    }

    suspend fun releaseFocus(consumer: EventConsumer) {
        withContext(dispatcher) {
            val existing = eventConsumers.value.find { it.consumer == consumer && !it.active }
            if (existing != null) {
                return@withContext
            }

            val consumerState: ConsumerState = getOrCreate(consumer)
            val consumers = eventConsumers.value.map {
                if (it.consumer == consumer) {
                    consumerState.copy(active = false)
                } else {
                    it
                }
            }

            eventConsumers.value = consumers
        }
    }

    suspend fun acquireFocus(consumer: EventConsumer) {
        withContext(dispatcher) {
            val existing = eventConsumers.value.find { it.consumer == consumer && it.active }
            if (existing != null) {
                return@withContext
            }

            val consumerState: ConsumerState = getOrCreate(consumer)
            val consumers = eventConsumers.value
                .filterNot { it.consumer == consumer }
                .toMutableList()
            consumers.add(0, consumerState.copy(active = true))
            eventConsumers.value = consumers
        }
    }

    private suspend fun getOrCreate(
        consumer: EventConsumer,
    ): ConsumerState = withContext(dispatcher) {
        val consumers: MutableList<ConsumerState> = eventConsumers.value.toMutableList()
        val result: ConsumerState = consumers.find { it.consumer == consumer } ?: run {
            val element = ConsumerState(
                active = false,
                consumer = consumer,
                events = MutableSharedFlow(),
            )
            consumers.add(element)
            element
        }

        eventConsumers.value = consumers
        result
    }

}

private data class ConsumerState(
    val active: Boolean,
    val consumer: EventConsumer,
    val events: MutableSharedFlow<GamepadButtonPress>,
)

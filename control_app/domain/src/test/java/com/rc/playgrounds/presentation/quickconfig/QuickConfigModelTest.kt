package com.rc.playgrounds.presentation.quickconfig

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.Config
import com.rc.playgrounds.config.model.ControlOffsets
import com.rc.playgrounds.config.stream.QualityProfile
import com.rc.playgrounds.control.quick.QuickConfigState
import com.rc.playgrounds.navigation.ActiveScreenProvider
import com.rc.playgrounds.navigation.Screen
import com.rc.playgrounds.remote.stream.StreamQualityProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

class QuickConfigModelTest {
    private val timeout = 10.seconds
    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        throw AssertionError("Uncaught exception inside coroutine", e)
    }
    private val scope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher() + exceptionHandler
    )
    private val screenState = MutableStateFlow<Screen>(Screen.MAIN)
    private val activeScreenProvider = mock<ActiveScreenProvider> {
        on { screen } doReturn screenState
    }
    private val configState = MutableStateFlow<Config>(createConfig())

    private fun createConfig(): Config {
        val offsets = mock<ControlOffsets> {
            on { steer } doReturn TEST_STEER_OFFSET
        }
        return mock<Config> {
            on { envOverrides } doReturn listOf(
                EnvironmentOverrides(
                    name = "indoor",
                    profiles = listOf(
                        OverrideProfile("default"),
                        OverrideProfile("sport"),
                    )
                ),
                EnvironmentOverrides(
                    name = "outdoor",
                    profiles = listOf(
                        OverrideProfile("default"),
                        OverrideProfile("sport"),
                        OverrideProfile("extreme"),
                    )
                ),
            )

            on { controlOffsets } doReturn offsets
        }
    }

    private val activeConfigProvider = mock<ActiveConfigProvider> {
        on { configFlow } doReturn configState
    }

    private val currentQuality: Flow<QualityProfile> = MutableStateFlow<QualityProfile>(
        QualityProfile(
            width = 1920,
            height = 1080,
            framerate = 30,
            bitrate = 3_000_000,
        ))
    private val qualityProvider = mock<StreamQualityProvider> {
        on { currentQuality } doReturn currentQuality
    }
    private val quickConfigOpened = MutableStateFlow(false)
    private val quickConfig = mock<QuickConfigState> {
        on { opened } doReturn quickConfigOpened
    }

    private val underTest = QuickConfigModel(
        scope,
        activeScreenProvider,
        activeConfigProvider,
        qualityProvider,
        quickConfig,
    )

    @Test
    fun smoke() = runTest {
        Assert.assertTrue(underTest.viewModel.first() is QuickConfigViewModel.Hidden)
    }

    @Test
    fun `dashboard visible`() = runTest(timeout = timeout) {
        quickConfigOpened.value = true
        val view = underTest.viewModel
            .filterIsInstance<QuickConfigViewModel.DashboardVisible>()
            .first()

        Assert.assertEquals(3, view.elementGroups.size)

        Assert.assertEquals(2, view.elementGroups[1].elements.size)
        Assert.assertFalse(view.elementGroups[1].focused)
        Assert.assertFalse(view.elementGroups[1].elements.first().focused)

        Assert.assertEquals(3, view.elementGroups[2].elements.size)
        Assert.assertFalse(view.elementGroups[2].focused)
        Assert.assertFalse(view.elementGroups[2].elements.first().focused)
    }
    @Test
    fun `dashboard steer offset built-in group`() = runTest(timeout = timeout) {
        quickConfigOpened.value = true
        val view = underTest.viewModel
            .filterIsInstance<QuickConfigViewModel.DashboardVisible>()
            .first()

        Assert.assertEquals(2, view.elementGroups[0].elements.size)
        Assert.assertTrue(view.elementGroups[0].focused)
        Assert.assertEquals("steer offset: 0.015", view.elementGroups[0].title)
        Assert.assertFalse(view.elementGroups[0].elements.first().focused)
    }

    @Test
    fun `dashboard move focus from column title to first element`() = runTest(timeout = timeout) {
        quickConfigOpened.value = true
        val focusDownView: QuickConfigViewModel.DashboardVisible = onView { v ->
            v.onButtonDownPressed()
        }

        Assert.assertFalse(focusDownView.elementGroups.first().focused)
        Assert.assertTrue(focusDownView.elementGroups.first().elements[0].focused)
    }

    @Test
    fun `dashboard move focus between columns`() = runTest(timeout = timeout) {
        quickConfigOpened.value = true

        onView { v ->
            v.onButtonRightPressed()
        }

        onView { v ->
            v.onButtonDownPressed()
        }


        val view = onView { v ->
            v.onButtonRightPressed()
        }

        Assert.assertFalse(view.elementGroups[1].focused)
        Assert.assertFalse(view.elementGroups[1].elements[0].focused)
        Assert.assertTrue("Second column must have focus! View: $view",view.elementGroups[2].focused)
        Assert.assertFalse(view.elementGroups[2].elements[0].focused)
    }

    @Test
    fun `dashboard move down trimming`() = runTest(timeout = timeout) {
        quickConfigOpened.value = true
        onView { v ->
            v.onButtonRightPressed()
        }
        val colIndex = 2

        val columnElements = onView { v ->
            v.onButtonRightPressed()
        }.elementGroups[colIndex].elements

        repeat(columnElements.size + 1) {
            onView { v ->
                println("move down! Column elements: '${v.elementGroups[colIndex].elements}'")
                v.onButtonDownPressed()
                configState.value = createConfig() // just to trigger view update
            }
        }
        val v = underTest.viewModel
            .filterIsInstance<QuickConfigViewModel.DashboardVisible>()
            .first()
        val elements = v.elementGroups[colIndex].elements
        Assert.assertTrue("Expecting last element focused: $columnElements", elements.last().focused)
    }

    private suspend fun onView(action: (QuickConfigViewModel.DashboardVisible) -> Unit):
            QuickConfigViewModel.DashboardVisible {
        val view = underTest.viewModel
            .filterIsInstance<QuickConfigViewModel.DashboardVisible>()
            .first()

        action(view)

        return underTest.viewModel
            .filterIsInstance<QuickConfigViewModel.DashboardVisible>()
            .first { it != view }
    }

}

private const val TEST_STEER_OFFSET = 0.0153f
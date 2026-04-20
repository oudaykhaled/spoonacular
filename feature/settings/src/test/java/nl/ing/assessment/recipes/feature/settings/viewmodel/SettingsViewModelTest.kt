package nl.ing.assessment.recipes.feature.settings.viewmodel

import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.ing.assessment.recipes.core.domain.model.ThemeMode
import nl.ing.assessment.recipes.core.domain.repository.SettingsRepository
import nl.ing.assessment.recipes.core.telemetry.EventTracker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    private val dynamicColorFlow = MutableStateFlow(false)

    private val repository: SettingsRepository = mockk(relaxed = true) {
        coEvery { themeMode } returns themeModeFlow
        coEvery { dynamicColor } returns dynamicColorFlow
        coEvery { setThemeMode(any()) } just Runs
        coEvery { setDynamicColor(any()) } just Runs
    }
    private val eventTracker: EventTracker = mockk(relaxed = true)

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `tracks screen view on init`() {
        createViewModel()

        verify { eventTracker.trackScreenView("SettingsScreen") }
    }

    @Test
    fun `state emits repository theme mode and dynamic color updates`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.state.test {
            assertEquals(SettingsUiState(themeMode = ThemeMode.SYSTEM, dynamicColorEnabled = false).themeMode, awaitItem().themeMode)

            themeModeFlow.value = ThemeMode.DARK
            advanceUntilIdle()
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)

            dynamicColorFlow.value = true
            advanceUntilIdle()
            assertEquals(true, awaitItem().dynamicColorEnabled)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onThemeModeChanged delegates to repository`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onThemeModeChanged(ThemeMode.LIGHT)
        advanceUntilIdle()

        coVerify { repository.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `onDynamicColorToggled delegates to repository`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onDynamicColorToggled(true)
        advanceUntilIdle()

        coVerify { repository.setDynamicColor(true) }
    }

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(
        repository = repository,
        eventTracker = eventTracker,
    )
}

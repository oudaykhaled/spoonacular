package nl.ing.assessment.recipes.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("MagicNumber")
@LargeTest
@RunWith(AndroidJUnit4::class)
class RecipesBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
    }

    @Test
    fun scrollRecipesList() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            startActivityAndWait()
            val searchBar = device.findObject(By.res("search_bar"))
            searchBar?.text = "pasta"
            device.wait(Until.hasObject(By.res("search_list")), 5_000)
        }
    ) {
        val list = device.findObject(By.res("search_list"))
        list?.setGestureMargin(device.displayWidth / 5)
        list?.fling(Direction.DOWN)
    }

    private companion object {
        const val PACKAGE_NAME = "nl.ing.assessment.recipes"
    }
}

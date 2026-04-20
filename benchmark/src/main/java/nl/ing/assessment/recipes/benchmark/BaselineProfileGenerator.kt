package nl.ing.assessment.recipes.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = "nl.ing.assessment.recipes") {
        startActivityAndWait()
        device.wait(Until.hasObject(By.res("search_bar")), 5_000)

        device.findObject(By.res("search_bar"))?.text = "pasta"
        device.wait(Until.hasObject(By.res("search_list")), 5_000)

        val searchList = device.findObject(By.res("search_list"))
        searchList?.setGestureMargin(device.displayWidth / 5)
        searchList?.fling(Direction.DOWN)
        searchList?.fling(Direction.UP)

        device.findObject(By.res("search_list"))?.let { list ->
            list.children.firstOrNull { it.isClickable }?.click()
                ?: list.click()
        }
        device.wait(Until.hasObject(By.res("details_screen")), 5_000)

        device.pressBack()
        device.wait(Until.hasObject(By.res("search_list")), 5_000)

        device.findObject(By.res("nav_favorites"))?.click()
        device.wait(Until.hasObject(By.text("Favorites")), 5_000)

        device.findObject(By.res("nav_settings"))?.click()
        device.wait(Until.hasObject(By.text("Settings")), 5_000)

        device.findObject(By.text("Dark"))?.click()
        device.waitForIdle()

        device.findObject(By.res("nav_search"))?.click()
        device.waitForIdle()
    }
}

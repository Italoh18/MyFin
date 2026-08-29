package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.CategoryColorProvider
import com.example.ui.components.PieChartMonthly
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CategorySlice
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        PieChartMonthly(
          monthName = "Agosto 2026",
          totalExpense = 750.40,
          slices = listOf(
            CategorySlice("Alimentação", CategoryColorProvider.getColorForName("Alimentação"), 350.0, 0.46f, 3),
            CategorySlice("Transporte", CategoryColorProvider.getColorForName("Transporte"), 200.40, 0.27f, 2),
            CategorySlice("Lazer", CategoryColorProvider.getColorForName("Lazer"), 200.0, 0.27f, 1)
          ),
          onPreviousMonth = {},
          onNextMonth = {},
          onResetMonth = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

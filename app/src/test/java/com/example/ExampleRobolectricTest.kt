package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kharcha Tracker", appName)
  }

  @Test
  fun `render main screen default`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MainViewModel(application)
    composeTestRule.setContent {
      MyApplicationTheme {
        MainScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }
}

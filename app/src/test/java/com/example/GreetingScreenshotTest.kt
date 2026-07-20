package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
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
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          Surface(
            shape = RoundedCornerShape(16dp),
            color = Color(0xFF1A1F26),
            modifier = Modifier
              .width(280dp)
              .padding(16dp)
          ) {
            Column(
              modifier = Modifier.padding(24dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "日本語",
                fontSize = 32sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "にほんご",
                fontSize = 16sp,
                color = Color(0xFF60A5FA),
                modifier = Modifier.padding(top = 8dp)
              )
              Text(
                text = "Japanese language",
                fontSize = 14sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 12dp)
              )
            }
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

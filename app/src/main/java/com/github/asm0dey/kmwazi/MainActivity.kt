package com.github.asm0dey.kmwazi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.asm0dey.kmwazi.di.ServiceLocator
import com.github.asm0dey.kmwazi.ui.KmwaziTheme
import com.github.asm0dey.kmwazi.ui.navigation.KmwaziNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
        ServiceLocator.initialize(applicationContext)
        setContent {
            KmwaziApp()
        }
    }
}

@Composable
fun KmwaziApp() {
    KmwaziTheme(useDarkTheme = true) {
        Surface {
            KmwaziNavHost(modifier = Modifier)
        }
    }
}

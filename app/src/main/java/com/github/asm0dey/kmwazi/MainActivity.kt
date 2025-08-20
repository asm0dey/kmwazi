package com.github.asm0dey.kmwazi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.github.asm0dey.kmwazi.data.SettingsRepository
import com.github.asm0dey.kmwazi.ui.KmwaziTheme
import com.github.asm0dey.kmwazi.ui.navigation.KmwaziNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KmwaziApp()
        }
    }
}

@Composable
fun KmwaziApp() {
    KmwaziTheme(useDarkTheme = true) {
        Surface(color = Color.Black) {
            KmwaziNavHost(modifier = Modifier)
        }
    }
}
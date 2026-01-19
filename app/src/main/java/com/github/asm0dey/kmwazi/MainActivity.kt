package com.github.asm0dey.kmwazi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.asm0dey.kmwazi.di.ServiceLocator
import com.github.asm0dey.kmwazi.ui.KmwaziTheme
import com.github.asm0dey.kmwazi.ui.navigation.KmwaziNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

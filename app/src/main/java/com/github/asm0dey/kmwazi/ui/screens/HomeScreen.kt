package com.github.asm0dey.kmwazi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.ui.navigation.Routes

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Kmwazi",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        Button(
            onClick = { onNavigate(Routes.Touch) },
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Start game")
            Text(" Start ", style = MaterialTheme.typography.labelLarge)
        }
        Button(
            onClick = { onNavigate(Routes.Settings) },
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
            Text(" Settings ", style = MaterialTheme.typography.labelLarge)
        }
        Button(
            onClick = { onNavigate(Routes.Help) },
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
            Text(" Help ", style = MaterialTheme.typography.labelLarge)
        }
    }
}

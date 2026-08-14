package com.homely.smarthome.feature.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SetupScreen() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Homely setup complete",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "The Firebase-ready Android foundation is connected to the shared data contract.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}


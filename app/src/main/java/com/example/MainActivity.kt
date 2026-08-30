package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.navigation.AppNavigation
import com.example.ui.theme.GameBridgeTheme
import com.example.viewmodel.GameBridgeViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GameBridgeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameBridgeTheme {
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}


package com.example.loglife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.loglife.ui.screens.MainScreen
import com.example.loglife.ui.theme.LoglifeTheme

import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity that hosts the LogLife application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoglifeTheme {
                
                MainScreen()
            }
        }
    }
}
package com.reevan.reevzmealz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.ui.ReevzMealzApp
import com.reevan.reevzmealz.ui.settings.PreferencesViewModel
import com.reevan.reevzmealz.ui.theme.ReevzMealzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Same instance Settings uses, so a theme change applies immediately.
            val preferences: PreferencesViewModel =
                viewModel(factory = PreferencesViewModel.Factory)
            val settings by preferences.settings.collectAsState()

            ReevzMealzTheme(themeMode = settings.themeMode) {
                ReevzMealzApp()
            }
        }
    }
}

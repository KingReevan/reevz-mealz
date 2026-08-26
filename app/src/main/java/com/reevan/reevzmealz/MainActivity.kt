package com.reevan.reevzmealz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.reevan.reevzmealz.ui.ReevzMealzApp
import com.reevan.reevzmealz.ui.theme.ReevzMealzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReevzMealzTheme {
                ReevzMealzApp()
            }
        }
    }
}

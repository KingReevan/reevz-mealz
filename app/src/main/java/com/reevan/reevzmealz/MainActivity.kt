package com.reevan.reevzmealz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.reevan.reevzmealz.ui.theme.ReevzMealzTheme
import com.reevan.reevzmealz.ui.today.TodayScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReevzMealzTheme {
                TodayScreen()
            }
        }
    }
}

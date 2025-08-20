package com.bn.bassemexpensetrackerlite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.compose.rememberNavController
import com.bn.bassemexpensetrackerlite.ui.navigation.AppNavHost
import com.bn.bassemexpensetrackerlite.ui.theme.BassemExpenseTrackerLiteTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BassemExpenseTrackerLiteTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}

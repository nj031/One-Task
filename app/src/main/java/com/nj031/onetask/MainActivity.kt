package com.nj031.onetask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nj031.onetask.navigation.OneTaskNavHost
import com.nj031.onetask.ui.theme.OneTaskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OneTaskTheme {
                OneTaskNavHost()
            }
        }
    }
}

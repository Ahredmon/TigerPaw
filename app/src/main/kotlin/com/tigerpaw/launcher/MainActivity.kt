package com.tigerpaw.launcher

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import com.tigerpaw.launcher.core.ui.theme.TigerPawTheme
import com.tigerpaw.launcher.navigation.TigerPawNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setContent {
            TigerPawTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    TigerPawNavHost()
                }
            }
        }
    }
}

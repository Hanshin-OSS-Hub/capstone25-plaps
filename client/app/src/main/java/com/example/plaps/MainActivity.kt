package com.example.plaps

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.plaps.ui.theme.PlapsTheme
import com.kakao.sdk.common.util.Utility

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlapsTheme {
                // 분리한 MainScreen을 호출
                MainAppScreen()
            }
        }

        // 키해시 보기 위함.
        val keyHash = Utility.getKeyHash(this)
        Log.d("KeyHash", keyHash)
    }
}
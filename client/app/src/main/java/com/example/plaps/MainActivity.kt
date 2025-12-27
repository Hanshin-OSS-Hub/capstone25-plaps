package com.example.plaps

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plaps.ui.theme.PlapsTheme
import com.kakao.sdk.common.util.Utility
import kotlinx.coroutines.delay
import dagger.hilt.android.AndroidEntryPoint // 👈 Hilt Import

@AndroidEntryPoint

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlapsTheme {
                // 바로 MainAppScreen으로 가지 않고, 진입점 함수를 거침
                PlapsAppEntry()
            }
        }

        // 키해시 보기 위함.
        val keyHash = Utility.getKeyHash(this)
        Log.d("KeyHash", keyHash)
    }
}

// 하단 앱 실행 로딩화면 - 박상우 작성

@Composable
fun PlapsAppEntry() {
    // 로딩 상태 관리
    var isLoading by remember { mutableStateOf(true) }

    // 앱 실행 시 한 번만 실행
    LaunchedEffect(Unit) {
        // 여기에 실제 데이터 로딩 상태를 넣을 수 있음(지금은 임시로 2000으로 설정했음)
        delay(2000)
        isLoading = false
    }

    // 상태에 따라 부드럽게 화면 전환 (Crossfade 애니메이션)
    Crossfade(targetState = isLoading, label = "SplashTransition") { loading ->
        if (loading) {
            SplashScreen()
        } else {
            MainAppScreen()
        }
    }
}

@Composable
fun SplashScreen() {
    // 로딩 화면 디자인
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4A80F0)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 나중에 이미지로 변경할 예정임(지금은 임시 아이콘으로 해놓음)
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "App Logo",
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 일단 PLAPS 이름으로 설정해놓음
            Text(
                text = "PLAPS",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
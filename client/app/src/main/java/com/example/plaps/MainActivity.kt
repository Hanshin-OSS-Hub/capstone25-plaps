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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.plaps.ui.theme.PlapsTheme
import com.example.plaps.ui.theme.ThemeViewModel
import com.kakao.sdk.common.util.Utility
import com.navercorp.nid.NaverIdLoginSDK
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🌟 [네이버 로그인 초기화]
        NaverIdLoginSDK.initialize(
            context = this,
            clientId = BuildConfig.NAVER_CLIENT_ID,
            clientSecret = BuildConfig.NAVER_CLIENT_SECRET,
            clientName = "PLAPS"
        )

        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()

            // 🌟 로그인 상태 관리를 위한 AuthViewModel 호출
            val authViewModel: AuthViewModel = hiltViewModel()

            PlapsTheme(darkTheme = isDarkMode) {
                // AuthViewModel을 Entry로 넘겨줍니다.
                PlapsAppEntry(authViewModel)
            }
        }

        // 키해시 확인용
        val keyHash = Utility.getKeyHash(this)
        Log.d("KeyHash", keyHash)
    }
}

@Composable
fun PlapsAppEntry(authViewModel: AuthViewModel) {
    var isLoading by remember { mutableStateOf(true) }

    // 🌟 ViewModel에서 로그인 상태 구독 (true면 로그인됨, false면 안 됨)
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        delay(2000)
        isLoading = false
    }

    Crossfade(targetState = isLoading, label = "SplashTransition") { loading ->
        if (loading) {
            SplashScreen()
        } else {
            // 🌟 로딩이 끝나면 로그인 상태에 따라 화면 분기!
            if (isLoggedIn) {
                // 로그인 성공 시 PLAPS 메인 화면으로
                // MainAppScreen 안에서 마이페이지로 갈 때도 authViewModel을 넘겨주면 좋습니다.
                MainAppScreen()
            } else {
                // 로그인 안 되어 있으면 로그인 화면으로
                LoginScreen(authViewModel = authViewModel)
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4A80F0)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "App Logo",
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

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
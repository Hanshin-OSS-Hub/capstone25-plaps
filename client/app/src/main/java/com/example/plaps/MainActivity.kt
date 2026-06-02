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
import androidx.lifecycle.lifecycleScope
import com.example.plaps.data.EventRepository
import com.example.plaps.ui.theme.PlapsTheme
import com.example.plaps.ui.theme.ThemeViewModel
import com.kakao.sdk.common.util.Utility
import com.navercorp.nid.NaverIdLoginSDK
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 🎯 [업적 버그 해결] Hilt를 통해 데이터 초기화를 위한 레포지토리 주입
    @Inject
    lateinit var eventRepository: EventRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🌟 [네이버 로그인 초기화]
        NaverIdLoginSDK.initialize(
            context = this,
            clientId = BuildConfig.NAVER_CLIENT_ID,
            clientSecret = BuildConfig.NAVER_CLIENT_SECRET,
            clientName = "PLAPS"
        )

        // 앱이 처음 켜질 때 백그라운드에서 4개 기본 업적을 DB에 자동으로 넣어줌
        lifecycleScope.launch {
            try {
                eventRepository.initDefaultAchievements()
                Log.d("MainActivity", "기본 업적 데이터 생성 완료")
            } catch (e: Exception) {
                Log.e("MainActivity", "업적 데이터 초기화 실패", e)
            }
        }

        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()

            // 로그인 상태 관리를 위한 AuthViewModel 호출
            val authViewModel: AuthViewModel = hiltViewModel()

            PlapsTheme(darkTheme = isDarkMode) {
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
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        delay(2000)
        isLoading = false
    }

    Crossfade(targetState = isLoading, label = "SplashTransition") { loading ->
        if (loading) {
            SplashScreen()
        } else {
            if (isLoggedIn) {
                MainAppScreen()
            } else {
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
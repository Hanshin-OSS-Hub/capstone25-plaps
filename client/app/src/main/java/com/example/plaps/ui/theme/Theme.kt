package com.example.plaps.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// PLAPS 앱의 시그니처 색상
val PlapsPrimaryLight = Color(0xFF4A80F0) // 기존에 사용하시던 파란색
val PlapsPrimaryDark = Color(0xFF7EAFFF)  // 다크모드용으로 살짝 밝고 부드럽게 조정한 파란색

// 1. 다크모드일 때 적용될 색상들
private val DarkColorScheme = darkColorScheme(
    primary = PlapsPrimaryDark,
    onPrimary = Color.Black,
    background = Color(0xFF121212),       // 아주 어두운 검정 배경
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),          // 카드나 바텀시트용 짙은 회색
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2C),   // 텍스트 필드 등 입력창 배경
    onSurfaceVariant = Color(0xFFBDBDBD)  // 서브 텍스트(아이콘, 설명 등) 색상
)

// 2. 라이트모드일 때 적용될 색상들
private val LightColorScheme = lightColorScheme(
    primary = PlapsPrimaryLight,
    onPrimary = Color.White,
    background = Color(0xFFF8F9FA),       // 마이페이지 전체 화면 등에서 쓰던 연회색
    onBackground = Color.Black,
    surface = Color.White,                // 카드, 바텀시트 배경 (흰색)
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF3F4F6),   // 일정 추가창 텍스트 필드 배경
    onSurfaceVariant = Color(0xFF757575)  // 서브 텍스트 색상
)

@Composable
fun PlapsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 브랜드 컬러(파란색)를 항상 일정하게 유지하기 위해 dynamicColor 기본값을 false로 둡니다.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Type.kt 파일에 있는 폰트 설정 연동
        content = content
    )
}
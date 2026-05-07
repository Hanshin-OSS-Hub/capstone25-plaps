package com.example.plaps

import android.content.Intent // 화면 이동 기능
import androidx.compose.ui.platform.LocalContext // 현재 화면 정보 가져오기
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.plaps.data.Event

// 각 탭의 제목과 아이콘 정보
enum class BottomNavItem(val title: String, val icon: ImageVector) {
    Home("메인", Icons.Default.Home),
    Calendar("캘린더", Icons.Default.DateRange),
    Map("지도", Icons.Default.Place),
    MyPage("마이페이지", Icons.Default.Notifications)
}

@Composable
fun MainAppScreen(viewModel: EventViewModel = hiltViewModel()) {
    var currentTab by remember { mutableStateOf(BottomNavItem.Home) }
    val events by viewModel.allEvents.collectAsStateWithLifecycle()

    //화면 이동을 위한 context 변수 (KakaoMapScreen 내부 로직에서 필요할 수 있음)
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                BottomNavItem.values().forEach { item ->
                    val isSelected = (currentTab == item)

                    NavigationBarItem(
                        selected = isSelected,

                        // 🌟 [수정 부분] 지도를 눌러도 Intent로 튕겨 나가지 않고 탭만 변경하도록 수정!
                        onClick = {
                            currentTab = item
                        },

                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,      // 선택 시 파란색
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer, // 선택된 아이콘 배경 원 색상
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, // 비선택 아이콘 색상
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                BottomNavItem.Home -> WeeklyHomeScreen(
                    events = events,
                    onSave = { viewModel.saveEvent(it) },
                    onDelete = { viewModel.deleteEvent(it) }
                )
                BottomNavItem.Calendar -> MonthlyCalendarTab(
                    events = events,
                    onSave = { viewModel.saveEvent(it) },
                    onDelete = { viewModel.deleteEvent(it) }
                )
                // 🌟 [수정 부분] PlaceholderScreen 대신 실제 지도를 호출합니다.
                BottomNavItem.Map -> KakaoMapScreen()

                BottomNavItem.MyPage -> {
                    val achievements by viewModel.allAchievements.collectAsStateWithLifecycle()
                    MyPageScreen(events = events, achievements = achievements)
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
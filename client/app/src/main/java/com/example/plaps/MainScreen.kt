package com.example.plaps

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.plaps.data.Event


 //각 탭의 제목과 아이콘 정보를 포함
enum class BottomNavItem(val title: String, val icon: ImageVector) {
    Home("메인", Icons.Default.Home),
    Calendar("캘린더", Icons.Default.DateRange),
    Map("지도", Icons.Default.Place),
    MyPage("마이페이지", Icons.Default.Notifications)
}

//Scaffold를 사용하여 상단/하단 바와 메인 컨텐츠 영역을 구성합니다.
@Composable
fun MainAppScreen(viewModel: EventViewModel = viewModel()) {
    // 현재 선택된 탭의 상태를 관리 (기본값: Home)
    // remember를 사용하여 재구성(Recomposition) 시에도 상태를 유지
    var currentTab by remember { mutableStateOf(BottomNavItem.Home) }

    // ViewModel의 Flow 데이터를 Compose의 State로 변환
    // DB에 데이터가 추가, 수정, 삭제 시 자동으로 events 변수가 업데이트되어 UI가 갱신
    val events by viewModel.allEvents.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(), // 상태바 영역 침범 방지
        bottomBar = {
            // 하단 네비게이션 바 UI 구성
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                // 정의해둔 Enum(BottomNavItem)을 순회하며 아이템 생성
                BottomNavItem.values().forEach { item ->
                    // 현재 탭과 아이템이 일치하는지 확인 (선택 상태 판별)
                    val isSelected = (currentTab == item)

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = item }, // 클릭 시 현재 탭 상태 업데이트
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title, fontSize = 10.sp) },
                        // 선택 여부에 따른 색상 커스텀 설정
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF4A80F0), // 선택된 아이콘 색상 (파란색 계열)
                            selectedTextColor = Color(0xFF4A80F0),
                            indicatorColor = Color(0xFFE8F0FE),    // 선택된 아이콘 배경 원 색상
                            unselectedIconColor = Color.Gray,      // 선택되지 않은 아이콘 색상
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // Scaffold의 innerPadding을 적용(하단 바에 가려지지 않도록 해줌)
        Box(modifier = Modifier.padding(innerPadding)) {
            // currentTab 상태에 따라 보여줄 화면을 교체 (탭 전환 로직)
            when (currentTab) {
                BottomNavItem.Home -> WeeklyHomeScreen(
                    events = events, // DB에서 가져온 이벤트 리스트 전달
                    onSave = { viewModel.saveEvent(it) },   // 저장 로직 연결
                    onDelete = { viewModel.deleteEvent(it) } // 삭제 로직 연결
                )
                BottomNavItem.Calendar -> MonthlyCalendarTab(
                    events = events,
                    onSave = { viewModel.saveEvent(it) },
                    onDelete = { viewModel.deleteEvent(it) }
                )
                // 아직 구현되지 않은 화면은 PlaceholderScreen으로 대체
                BottomNavItem.Map -> PlaceholderScreen("지도 화면")
                BottomNavItem.MyPage -> PlaceholderScreen("마이페이지")
            }
        }
    }
}
@Composable
fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    }
}
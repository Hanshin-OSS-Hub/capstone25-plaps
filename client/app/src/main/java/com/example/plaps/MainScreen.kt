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

enum class BottomNavItem(val title: String, val icon: ImageVector) {
    Home("메인", Icons.Default.Home),
    Calendar("캘린더", Icons.Default.DateRange),
    Map("지도", Icons.Default.Place),
    Alert("알림", Icons.Default.Notifications)
}

@Composable
fun MainAppScreen(viewModel: EventViewModel = viewModel()) {
    var currentTab by remember { mutableStateOf(BottomNavItem.Home) }

    // DB 데이터 관찰 (Flow -> State)
    val events by viewModel.allEvents.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                BottomNavItem.values().forEach { item ->
                    val isSelected = (currentTab == item)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = item },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF4A80F0),
                            selectedTextColor = Color(0xFF4A80F0),
                            indicatorColor = Color(0xFFE8F0FE),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
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
                BottomNavItem.Map -> PlaceholderScreen("지도 화면")
                BottomNavItem.Alert -> PlaceholderScreen("알림 화면")
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
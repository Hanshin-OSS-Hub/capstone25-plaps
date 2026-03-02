package com.example.plaps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plaps.data.Achievement
import com.example.plaps.data.Event

@Composable
fun MyPageScreen(events: List<Event>, achievements: List<Achievement>) {
    // DB에서 'isCompleted = true'인 일정의 개수를 계산
    val completedEventsCount = events.count { it.isCompleted }

    // 전체 스크롤 위해서 LazyColumn 사용
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)) // 전체 배경 연한 회색
    ) {
        // 1. 프로필 헤더 (파란색 영역)
        item { ProfileHeader() }

        // 2. 완료한 일정 요약 카드
        item { SummaryCard(completedEventsCount) }

        // 3. 업적 타이틀
        item {
            Text(
                text = "업적",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }

        // 4. 업적 리스트
        val displayAchievements = achievements
        items(displayAchievements) { achievement ->
            AchievementItem(achievement)
        }

        // 5. 설정 영역
        item { SettingsSection() }
    }
}

@Composable
fun ProfileHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF4A80F0)) // 파란색 배경
            .padding(top = 32.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
    ) {
        Text("마이페이지", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 원형 프로필 이미지(나중에 사용자가 변경 가능하도록 수정할 예정)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PersonOutline, contentDescription = "프로필", tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("USER", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Plaps@hs.ac.kr", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SummaryCard(completedCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 트로피 아이콘
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "완료", tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("완료한 일정", color = Color.Gray, fontSize = 14.sp)
                    Text("$completedCount", fontSize = 36.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("업적을 완료하고 칭호를 획득하세요!", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement) {
    val isUnlocked = achievement.isUnlocked
    // 상태에 따른 색상 지정
    val borderColor = if (isUnlocked) Color(0xFFFFC107) else Color(0xFFEEEEEE)
    val iconBgColor = if (isUnlocked) Color(0xFFFFF8E1) else Color(0xFFF5F5F5)
    val iconColor = if (isUnlocked) Color(0xFFFFC107) else Color(0xFFBDBDBD)
    val borderStroke = if (isUnlocked) BorderStroke(2.dp, borderColor) else BorderStroke(1.dp, borderColor)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = borderStroke,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Adjust, contentDescription = null, tint = iconColor)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(achievement.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isUnlocked) Color.Black else Color.Gray)
                        if (isUnlocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(achievement.description, fontSize = 12.sp, color = Color.Gray)
                }
            }

            // 달성 전이면 프로그레스 바 표시
            if (!isUnlocked) {
                Spacer(modifier = Modifier.height(16.dp))
                val progress = if (achievement.goalValue > 0) achievement.currentValue.toFloat() / achievement.goalValue.toFloat() else 0f
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${achievement.currentValue}/${achievement.goalValue}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${(progress * 100).toInt()}%", fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color.Gray,
                    trackColor = Color(0xFFEEEEEE)
                )
            }
        }
    }
}

@Composable
fun SettingsSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("설정", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray, modifier = Modifier.padding(bottom = 8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                SettingsSwitchItem("알림", "일정 알림 받기", Icons.Default.NotificationsNone, Color(0xFFE3F2FD), Color(0xFF2196F3), true)
                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                SettingsSwitchItem("다크모드", "어두운 테마 사용", Icons.Default.DarkMode, Color(0xFFF5F5F5), Color(0xFF757575), false)
                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                SettingsArrowItem("언어", "한국어", Icons.Default.Language, Color(0xFFE8F5E9), Color(0xFF4CAF50))
                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                SettingsArrowItem("일반 설정", "앱 설정 및 정보", Icons.Default.Settings, Color(0xFFF3E5F5), Color(0xFF9C27B0))
            }
        }
        Spacer(modifier = Modifier.height(32.dp)) // 하단 여백
    }
}

@Composable
fun SettingsSwitchItem(title: String, subtitle: String, icon: ImageVector, iconBg: Color, iconTint: Color, initialChecked: Boolean) {
    var checked by remember { mutableStateOf(initialChecked) }
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color.Black)
        )
    }
}

@Composable
fun SettingsArrowItem(title: String, subtitle: String, icon: ImageVector, iconBg: Color, iconTint: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "이동", tint = Color.Gray)
    }
}



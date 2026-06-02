package com.example.plaps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.plaps.data.Achievement
import com.example.plaps.data.Event
import com.example.plaps.ui.theme.ThemeViewModel

@Composable
fun MyPageScreen(
    events: List<Event>,
    achievements: List<Achievement>,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()

    val userEmail by authViewModel.userEmail.collectAsStateWithLifecycle()
    val nickname by authViewModel.nickname.collectAsStateWithLifecycle()
    val profileImageUri by authViewModel.profileImageUri.collectAsStateWithLifecycle()

    val completedEventsCount = events.count { it.isCompleted }

    var showNicknameDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { authViewModel.updateProfileImage(it.toString()) }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            ProfileHeader(
                userEmail = userEmail,
                nickname = nickname,
                profileImageUri = profileImageUri,
                onProfileImageClick = {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onNicknameClick = { showNicknameDialog = true }
            )
        }

        item { SummaryCard(completedEventsCount) }

        item {
            Text(
                text = "업적",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }

        items(
            items = achievements,
            key = { it.title },
            contentType = { "Achievement" }
        ) { achievement ->
            AchievementItem(achievement)
        }

        item {
            SettingsSection(
                isDarkMode = isDarkMode,
                onDarkModeToggle = { themeViewModel.toggleDarkMode(it) },
                onLogout = { authViewModel.logout() }
            )
        }
    }

    if (showNicknameDialog) {
        var textState by remember { mutableStateOf(nickname) }

        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("닉네임 변경", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = { Text("새 닉네임 입력") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textState.isNotBlank()) {
                        authViewModel.updateNickname(textState)
                    }
                    showNicknameDialog = false
                }) {
                    Text("변경", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) {
                    Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun ProfileHeader(
    userEmail: String?,
    nickname: String,
    profileImageUri: String?,
    onProfileImageClick: () -> Unit,
    onNicknameClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(top = 32.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
    ) {
        Text("마이페이지", color = MaterialTheme.colorScheme.onPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                    .clickable { onProfileImageClick() },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "프로필 이미지",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.PersonOutline, contentDescription = "프로필", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onNicknameClick() }
                        .padding(vertical = 4.dp)
                ) {
                    Text(nickname, color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.Edit, contentDescription = "수정", tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "계정: ${userEmail ?: "불러오는 중..."}", // 유저 확인용 텍스트 명확화
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "완료", tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("완료한 일정", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text("$completedCount", fontSize = 36.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("업적을 완료하고 칭호를 획득하세요!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement) {
    val isUnlocked = achievement.isUnlocked
    val iconBgColor = if (isUnlocked) Color(0xFFFFC107).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    val iconColor = if (isUnlocked) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
    val borderStroke = if (isUnlocked) BorderStroke(2.dp, Color(0xFFFFC107)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        Text(
                            achievement.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isUnlocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(achievement.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (!isUnlocked) {
                Spacer(modifier = Modifier.height(16.dp))
                val progress = if (achievement.goalValue > 0) achievement.currentValue.toFloat() / achievement.goalValue.toFloat() else 0f
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${achievement.currentValue}/${achievement.goalValue}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${(progress * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSection(isDarkMode: Boolean, onDarkModeToggle: (Boolean) -> Unit, onLogout: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("설정", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                SettingsSwitchItem("알림", "일정 알림 받기", Icons.Default.NotificationsNone, Color(0xFF2196F3), checked = true, onCheckedChange = null)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                SettingsSwitchItem("다크모드", "어두운 테마 사용", Icons.Default.DarkMode, Color(0xFF757575), checked = isDarkMode, onCheckedChange = onDarkModeToggle)

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                SettingsArrowItem("언어", "한국어", Icons.Default.Language, Color(0xFF4CAF50))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                SettingsActionItem("로그아웃", "계정에서 로그아웃 합니다", Icons.Default.Logout, Color.Red, onLogout)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?
) {
    var localChecked by remember { mutableStateOf(checked) }
    val currentChecked = if (onCheckedChange != null) checked else localChecked

    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = currentChecked,
            onCheckedChange = {
                if (onCheckedChange != null) {
                    onCheckedChange(it)
                } else {
                    localChecked = it
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SettingsArrowItem(title: String, subtitle: String, icon: ImageVector, iconTint: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "이동", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsActionItem(title: String, subtitle: String, icon: ImageVector, iconTint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = iconTint)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
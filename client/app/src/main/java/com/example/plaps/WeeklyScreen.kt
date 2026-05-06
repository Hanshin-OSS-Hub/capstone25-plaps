package com.example.plaps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plaps.data.Event
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


// 주간 메인 화면
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WeeklyHomeScreen(
    events: List<Event>,      // 전체 일정 리스트
    onSave: (Event) -> Unit,  // 저장(추가/수정)
    onDelete: (Event) -> Unit // 삭제
) {
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val scope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    var currentWeekStart by remember {
        mutableStateOf(LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value % 7.toLong()))
    }

    var editingEvent by remember { mutableStateOf<Event?>(null) }

    ModalBottomSheetLayout(
        sheetState = modalSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        // 바텀 시트 배경 테마 연동
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            AddOrEditEventSheet(
                selectedDate = selectedDate,
                existingEvent = editingEvent,
                onClose = { scope.launch { modalSheetState.hide() } },
                onSave = { newEvent ->
                    onSave(newEvent)
                    scope.launch { modalSheetState.hide() }
                },
                onDelete = { eventToDelete ->
                    onDelete(eventToDelete)
                    scope.launch { modalSheetState.hide() }
                }
            )
        }
    ) {
        // 전체 배경 테마 연동
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // 1. 상단 주간 달력 헤더
            WeeklyCalendarHeader(
                currentWeekStart = currentWeekStart,
                selectedDate = selectedDate,
                events = events,
                onDateSelected = { newDate -> selectedDate = newDate },
                onPrevWeek = { currentWeekStart = currentWeekStart.minusWeeks(1) },
                onNextWeek = { currentWeekStart = currentWeekStart.plusWeeks(1) },
                onAddEvent = {
                    editingEvent = null
                    scope.launch { modalSheetState.show() }
                }
            )

            // 2. 하단 일정 리스트
            WeeklyScheduleList(
                selectedDate = selectedDate,
                events = events.filter { it.date == selectedDate },
                onEventClick = { event ->
                    editingEvent = event
                    scope.launch { modalSheetState.show() }
                }
            )
        }
    }
}

// 상단 주간 달력 헤더
@Composable
fun WeeklyCalendarHeader(
    currentWeekStart: LocalDate,
    selectedDate: LocalDate,
    events: List<Event>,
    onDateSelected: (LocalDate) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onAddEvent: () -> Unit
) {
    val monthFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월")
    val weekFormatter = DateTimeFormatter.ofPattern("MM/dd")

    val weekStartStr = currentWeekStart.format(weekFormatter)
    val weekEndStr = currentWeekStart.plusDays(6).format(weekFormatter)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 파란색 영역 -> 테마의 primary 적용
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        // 상단: 앱 이름 + 현재 월 + 추가 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("PLAPS", color = MaterialTheme.colorScheme.onPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(selectedDate.format(monthFormatter), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 14.sp)
            }
            // 일정 추가 버튼 (onPrimary 배경 위에서 강조)
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onAddEvent() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "일정 추가", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 주 변경 아이콘 및 텍스트 색상 연동
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevWeek) { Icon(Icons.Default.ChevronLeft, contentDescription = "이전 주", tint = MaterialTheme.colorScheme.onPrimary) }
            Text("$weekStartStr - $weekEndStr", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onNextWeek) { Icon(Icons.Default.ChevronRight, contentDescription = "다음 주", tint = MaterialTheme.colorScheme.onPrimary) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 하단: 요일별 날짜 아이템
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            (0..6).forEach { i ->
                val date = currentWeekStart.plusDays(i.toLong())
                val hasEvent = events.any { it.date == date }

                WeeklyDayItem(
                    date = date,
                    isSelected = (date == selectedDate),
                    hasEvent = hasEvent,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}


// 개별 날짜 아이템
@Composable
fun WeeklyDayItem(
    date: LocalDate,
    isSelected: Boolean,
    hasEvent: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
    val dayOfMonth = date.dayOfMonth.toString()

    // 선택 시 날짜 박스 색상을 onPrimary(흰색 계열)로, 글자를 primary(파란색 계열)로 반전
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onDateSelected(date) }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(dayOfWeek, color = contentColor.copy(alpha = 0.8f), fontSize = 12.sp)
        Text(dayOfMonth, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)

        if (hasEvent) {
            Spacer(modifier = Modifier.height(4.dp))
            // 일정 점(dot) 색상 연동
            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(contentColor))
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}


// 하단 일정 리스트 화면
@Composable
fun WeeklyScheduleList(
    selectedDate: LocalDate,
    events: List<Event>,
    onEventClick: (Event) -> Unit
) {
    val titleFormatter = DateTimeFormatter.ofPattern("MM월 dd일 EEEE", Locale.KOREAN)

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 연한 회색 대신 시스템 배경색 적용
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 텍스트 색상 연동 (onBackground)
            Text(selectedDate.format(titleFormatter), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("${events.size}개 일정", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (events.isEmpty()) {
            EmptyScheduleView()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(events) { event ->
                    EventItem(event, onEventClick)
                }
            }
        }
    }
}
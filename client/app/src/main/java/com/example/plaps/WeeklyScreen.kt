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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
// 주간 달력 헤더 + 해당 날짜의 일정 리스트
@OptIn(ExperimentalMaterialApi::class) // Material2의 BottomSheet 사용을 위한 어노테이션
@Composable
fun WeeklyHomeScreen(
    events: List<Event>,      // 전체 일정 리스트
    onSave: (Event) -> Unit,  // 저장(추가/수정)
    onDelete: (Event) -> Unit // 삭제
) {
    // 바텀 시트 상태 관리 (숨김/보임)
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true // 반만 열리는 상태 건너뛰기
    )
    val scope = rememberCoroutineScope() // 시트 열기, 닫기를 위한 스코프

    // 현재 선택된 날짜 (기본값: 오늘)
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // 현재 보여지는 주(Week)의 시작일 계산
    var currentWeekStart by remember {
        mutableStateOf(LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value % 7.toLong()))
    }

    // 현재 수정 중인 이벤트
    var editingEvent by remember { mutableStateOf<Event?>(null) }

    // 바텀 시트 레이아웃
    ModalBottomSheetLayout(
        sheetState = modalSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = Color.White,
        sheetContent = {
            // 바텀 시트 내부 내용: 일정 추가/수정
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
        // 메인 컨텐츠 영역 (헤더 + 리스트)
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 상단 주간 달력 헤더
            WeeklyCalendarHeader(
                currentWeekStart = currentWeekStart,
                selectedDate = selectedDate,
                events = events,
                onDateSelected = { newDate -> selectedDate = newDate }, // 날짜 클릭 시 선택된 날짜 변경
                onPrevWeek = { currentWeekStart = currentWeekStart.minusWeeks(1) }, // 이전 주 이동
                onNextWeek = { currentWeekStart = currentWeekStart.plusWeeks(1) },  // 다음 주 이동
                onAddEvent = {
                    editingEvent = null // 새 추가 모드로 설정
                    scope.launch { modalSheetState.show() }
                }
            )

            // 2. 하단 일정 리스트 (선택된 날짜의 일정만 필터링)
            WeeklyScheduleList(
                selectedDate = selectedDate,
                events = events.filter { it.date == selectedDate },
                onEventClick = { event ->
                    editingEvent = event // 수정 모드로 설정
                    scope.launch { modalSheetState.show() }
                }
            )
        }
    }
}

 // 상단 주간 달력 헤더
// 앱 타이틀, 월 표시, 주 이동 버튼, 요일별 날짜 표시

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
    // 날짜 포맷터
    val monthFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월")
    val weekFormatter = DateTimeFormatter.ofPattern("MM/dd")

    // 현재 표시된 주의 시작일과 종료일
    val weekStartStr = currentWeekStart.format(weekFormatter)
    val weekEndStr = currentWeekStart.plusDays(6).format(weekFormatter)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF4A80F0)) // 파란색 배경
            .padding(16.dp)
    ) {
        // 상단: 앱 이름 + 현재 월 + 추가 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("PLAPS", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(selectedDate.format(monthFormatter), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
            // 일정 추가 버튼 (흰색 박스)
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onAddEvent() },
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "일정 추가", tint = Color(0xFF4A80F0))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 주 변경
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevWeek) { Icon(Icons.Default.ChevronLeft, contentDescription = "이전 주", tint = Color.White) }
            Text("$weekStartStr - $weekEndStr", color = Color.White, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onNextWeek) { Icon(Icons.Default.ChevronRight, contentDescription = "다음 주", tint = Color.White) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 하단: 요일별 날짜 아이템 (7일 단위 표시)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            (0..6).forEach { i ->
                val date = currentWeekStart.plusDays(i.toLong())
                // 해당 날짜에 일정이 있는지 확인
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


// 개별 날짜 아이템 (요일, 일 표시)
// 선택 시 흰색 배경 활성화
// 일정이 있으면 하단에 점(dot) 표시
@Composable
fun WeeklyDayItem(
    date: LocalDate,
    isSelected: Boolean,
    hasEvent: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN) // 월, 화, 수...
    val dayOfMonth = date.dayOfMonth.toString()

    // 선택 여부에 따른 색상 변경
    val backgroundColor = if (isSelected) Color.White else Color.Transparent
    val contentColor = if (isSelected) Color(0xFF4A80F0) else Color.White

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

        // 일정 유무 표시 (작은 원)
        if (hasEvent) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(contentColor))
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}


 // 하단 일정 리스트 화면
 // 선택된 날짜의 상세 일정 목록 표시
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
            .background(Color(0xFFF8F9FA)) // 연한 회색
            .padding(16.dp)
    ) {
        // 날짜 제목 및 일정 개수
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selectedDate.format(titleFormatter), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("${events.size}개 일정", fontSize = 14.sp, color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (events.isEmpty()) {
            // 일정이 없을 때 표시할 화면 (구현 필요)
            EmptyScheduleView()
        } else {
            // 일정 리스트
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(events) { event ->
                    EventItem(event, onEventClick) // 개별 일정 아이템 (구현 필요)
                }
            }
        }
    }
}
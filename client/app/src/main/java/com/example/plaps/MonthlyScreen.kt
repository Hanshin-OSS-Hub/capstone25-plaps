package com.example.plaps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plaps.data.Event
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// 월간 캘린더 탭 메인 화면
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MonthlyCalendarTab(
    events: List<Event>,      // 전체 일정 리스트
    onSave: (Event) -> Unit,  // 저장(추가/수정)
    onDelete: (Event) -> Unit // 삭제
) {
    val modalSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()
    var editingEvent by remember { mutableStateOf<Event?>(null) }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var searchText by remember { mutableStateOf("") }

    ModalBottomSheetLayout(
        sheetState = modalSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        // 바텀시트 배경 테마 연동
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
            // 1. 상단 헤더
            MonthViewHeader(
                searchText = searchText,
                onSearchChange = { searchText = it },
                onAddEvent = {
                    editingEvent = null
                    scope.launch { modalSheetState.show() }
                }
            )

            // 2. 월 이동
            MonthNavigation(
                currentMonth = currentMonth,
                onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
            )

            // 3. 요일 헤더
            DaysOfWeekHeader()

            // 4. 달력 본문
            MonthGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                events = events,
                onDateSelected = { selectedDate = it }
            )

            // 달력과 일정 리스트 사이 구분선 (Material 3 권장 방식으로 교체 및 테마 연동)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 8.dp)

            // 5. 선택된 날짜의 일정 리스트
            val dailyEvents = events.filter { it.date == selectedDate }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                ScheduleListForMonth(
                    selectedDate = selectedDate,
                    events = dailyEvents,
                    onEventClick = { event ->
                        editingEvent = event
                        scope.launch { modalSheetState.show() }
                    }
                )
            }
        }
    }
}

// 달력 그리드
@Composable
fun MonthGrid(currentMonth: YearMonth, selectedDate: LocalDate, events: List<Event>, onDateSelected: (LocalDate) -> Unit) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startOffset = firstDayOfMonth.dayOfWeek.value % 7
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    val dayOfMonth = index - startOffset + 1

                    if (dayOfMonth in 1..daysInMonth) {
                        val date = currentMonth.atDay(dayOfMonth)
                        val isSelected = (date == selectedDate)
                        val isToday = (date == LocalDate.now())
                        val hasEvent = events.any { it.date == date }

                        // 요일별 색상 지정 테마 연동 (일: 에러색, 토: 메인색, 평일: 기본 텍스트색)
                        val baseTextColor = when (col) {
                            0 -> MaterialTheme.colorScheme.error
                            6 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        // 선택된 경우 흰색(onPrimary)
                        val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else baseTextColor

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.4f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                // 선택 배경색 테마 연동
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                // 오늘 날짜 테두리 테마 연동
                                .then(if (isToday && !isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier)
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = dayOfMonth.toString(),
                                    color = textColor,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                )
                                // 일정이 있는 경우 하단 바
                                if (hasEvent) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(modifier = Modifier
                                        .width(16.dp)
                                        .height(3.dp)
                                        // 하단 바 색상 연동
                                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(5.dp))
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// 상단 헤더
@Composable
fun MonthViewHeader(searchText: String, onSearchChange: (String) -> Unit, onAddEvent: () -> Unit) {
    // 배경을 primary로 연동
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("캘린더", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            // 추가 버튼 디자인 연동
            Surface(modifier = Modifier.size(36.dp).clickable { onAddEvent() }, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "추가", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 검색창 색상 연동
        TextField(
            value = searchText,
            onValueChange = onSearchChange,
            placeholder = { Text("날짜 검색 (예: 11/11)", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), fontSize = 20.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary) },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                unfocusedContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onPrimary,
                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
    }
}

// 월 바꾸기 기능
@Composable
fun MonthNavigation(currentMonth: YearMonth, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        // 아이콘 및 텍스트 색상 연동
        IconButton(onClick = onPrevMonth) { Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달", tint = MaterialTheme.colorScheme.onBackground) }
        val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월", Locale.KOREAN)
        Text(currentMonth.format(formatter), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 16.dp))
        IconButton(onClick = onNextMonth) { Icon(Icons.Default.ChevronRight, contentDescription = "다음 달", tint = MaterialTheme.colorScheme.onBackground) }
    }
}

// 요일 헤더
@Composable
fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val days = listOf("일", "월", "화", "수", "목", "금", "토")
        days.forEachIndexed { index, day ->
            // 요일 텍스트 색상 연동
            val textColor = when (index) {
                0 -> MaterialTheme.colorScheme.error // 일요일
                6 -> MaterialTheme.colorScheme.primary // 토요일
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = textColor, fontSize = 13.sp)
        }
    }
}

// 달력 하단 일정 리스트
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListForMonth(selectedDate: LocalDate, events: List<Event>, onEventClick: (Event) -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("MM월 dd일 일정", Locale.KOREAN)
    Column(modifier = Modifier.fillMaxSize()) {
        // 텍스트 색상 연동
        Text(text = selectedDate.format(formatter), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp))

        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("일정이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    EventItem(event = event, onClick = onEventClick)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
package com.example.plaps

import androidx.compose.foundation.BorderStroke
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


//월간 캘린더 탭 메인 화면
// 월별 달력, 선택된 날짜의 일정 리스트를 표시
// 일정 추가/수정을 위한 BottomSheet 포함
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MonthlyCalendarTab(
    events: List<Event>,      // 전체 일정 리스트
    onSave: (Event) -> Unit,  // 저장(추가/수정)
    onDelete: (Event) -> Unit // 삭제
) {
    // 바텀 시트 및 CoroutineScope 설정
    val modalSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()
    var editingEvent by remember { mutableStateOf<Event?>(null) } // 수정할 이벤트 저장용

    // 캘린더 상태 관리
    var currentMonth by remember { mutableStateOf(YearMonth.now()) } // 현재 보고 있는 달
    var selectedDate by remember { mutableStateOf(LocalDate.now()) } // 사용자가 선택한 날짜
    var searchText by remember { mutableStateOf("") } // 검색어 상태

    ModalBottomSheetLayout(
        sheetState = modalSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = Color.White,
        sheetContent = {
            // 일정 추가/수정 시트 내용
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
        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            // 1. 상단 헤더 (검색창 + 추가 버튼)
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

            // 3. 요일 헤더 (일 월 화 수 목 금 토, 7일)
            DaysOfWeekHeader()

            // 4. 달력 본문
            MonthGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                events = events,
                onDateSelected = { selectedDate = it }
            )

            Divider(color = Color(0xFFEEEEEE), thickness = 8.dp)

            // 5. 선택된 날짜의 일정 리스트 (하단 영역)
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

// 달력 그리드 (날짜 칸 생성 로직)
@Composable
fun MonthGrid(currentMonth: YearMonth, selectedDate: LocalDate, events: List<Event>, onDateSelected: (LocalDate) -> Unit) {
    // 달력 계산 로직
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startOffset = firstDayOfMonth.dayOfWeek.value % 7 // 1일이 시작되기 전 빈 칸 개수 (일요일=0 기준)
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7 // 필요한 행의 개수 계산

    Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    val dayOfMonth = index - startOffset + 1 // 실제 날짜 계산

                    if (dayOfMonth in 1..daysInMonth) {
                        // 유효한 날짜인 경우
                        val date = currentMonth.atDay(dayOfMonth)
                        val isSelected = (date == selectedDate)
                        val isToday = (date == LocalDate.now())
                        val hasEvent = events.any { it.date == date }

                        // 요일별 색상 지정 (일: 빨강, 토: 파랑, 평일: 검정)
                        val baseTextColor = when (col) {
                            0 -> Color.Red
                            6 -> Color(0xFF4A80F0)
                            else -> Color.Black
                        }
                        // 선택된 경우 흰색 텍스트
                        val textColor = if (isSelected) Color.White else baseTextColor

                        // 날짜 셀 UI
                        Box(
                            modifier = Modifier
                                .weight(1f) // 균등 분할
                                .aspectRatio(1.4f) // 셀 비율 조정
                                .padding(2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF4A80F0) else Color.Transparent) // 선택 배경색
                                .then(if (isToday && !isSelected) Modifier.border(1.5.dp, Color(0xFF4A80F0), RoundedCornerShape(10.dp)) else Modifier) // 오늘 날짜 테두리
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
                                // 일정이 있는 경우 하단에 바(Bar) 표시
                                if (hasEvent) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(modifier = Modifier
                                        .width(16.dp)
                                        .height(3.dp)
                                        .background(if (isSelected) Color.White else Color(0xFF4A80F0), RoundedCornerShape(2.dp))
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(5.dp)) // 자리 확보용
                                }
                            }
                        }
                    } else {
                        // 날짜가 아닌 빈 공간 (달력 앞뒤 공백)
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}


// 상단부: 타이틀, 추가 버튼, 검색창
@Composable
fun MonthViewHeader(searchText: String, onSearchChange: (String) -> Unit, onAddEvent: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF4A80F0)).padding(16.dp)) {
        // 타이틀 및 추가 버튼
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("캘린더", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Surface(modifier = Modifier.size(36.dp).clickable { onAddEvent() }, shape = RoundedCornerShape(8.dp), color = Color.White) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "추가", tint = Color(0xFF4A80F0))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 검색창 (투명 배경 스타일)
        TextField(
            value = searchText,
            onValueChange = onSearchChange,
            placeholder = { Text("날짜 검색 (예: 11/11)", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.2f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
    }
}

//월 바꾸기 기능 (이전 달 / 다음 달)
@Composable
fun MonthNavigation(currentMonth: YearMonth, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevMonth) { Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달") }
        val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월", Locale.KOREAN)
        Text(currentMonth.format(formatter), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        IconButton(onClick = onNextMonth) { Icon(Icons.Default.ChevronRight, contentDescription = "다음 달") }
    }
}

// 요일 표시 헤더 (일 ~ 토, 7일)
@Composable
fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val days = listOf("일", "월", "화", "수", "목", "금", "토")
        days.forEachIndexed { index, day ->
            val textColor = when (index) {
                0 -> Color.Red // 일요일
                6 -> Color(0xFF4A80F0) // 토요일
                else -> Color.Gray
            }
            Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = textColor, fontSize = 13.sp)
        }
    }
}

// 선택된 날짜의 일정 리스트 (달력 하단에 위치)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListForMonth(selectedDate: LocalDate, events: List<Event>, onEventClick: (Event) -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("MM월 dd일 일정", Locale.KOREAN)
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = selectedDate.format(formatter), fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp))

        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("일정이 없습니다.", color = Color.Gray, fontSize = 14.sp)
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
                item { Spacer(modifier = Modifier.height(16.dp)) } // 하단 여백
            }
        }
    }
}
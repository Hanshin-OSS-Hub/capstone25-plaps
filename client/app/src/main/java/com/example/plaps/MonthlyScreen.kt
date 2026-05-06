package com.example.plaps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

    // 🌟 화면 깜빡임 없이 부드럽게 키보드를 제어하기 위한 매니저와 컨트롤러
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheetLayout(
        sheetState = modalSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
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
        // 🌟 빈 공간 터치 시 키보드를 부드럽게 내리는 로직 추가
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        keyboardController?.hide() // 1. 키보드 먼저 숨기기 (깜빡임 방지)
                        focusManager.clearFocus()  // 2. 포커스 해제
                    })
                }
        ) {
            // 1. 상단 헤더
            MonthViewHeader(
                searchText = searchText,
                onSearchChange = { newText ->
                    searchText = newText // 타이핑 중에는 검색어 상태만 업데이트
                },
                onSearchExecute = {
                    // 키보드의 돋보기/검색 버튼을 눌렀을 때만 날짜 검색 실행
                    val cleanText = searchText.trim()
                    val mmddRegex = Regex("^(\\d{1,2})[/.\\-](\\d{1,2})$") // 예: 11/11, 11.11, 11-11
                    val yyyymmddRegex = Regex("^(\\d{4})[/.\\-](\\d{1,2})[/.\\-](\\d{1,2})$") // 예: 2026/11/11

                    var parsedYear = currentMonth.year
                    var parsedMonth: Int? = null
                    var parsedDay: Int? = null

                    if (yyyymmddRegex.matches(cleanText)) {
                        val match = yyyymmddRegex.find(cleanText)!!
                        parsedYear = match.groupValues[1].toInt()
                        parsedMonth = match.groupValues[2].toInt()
                        parsedDay = match.groupValues[3].toInt()
                    } else if (mmddRegex.matches(cleanText)) {
                        val match = mmddRegex.find(cleanText)!!
                        parsedMonth = match.groupValues[1].toInt()
                        parsedDay = match.groupValues[2].toInt()
                    }

                    // 유효한 날짜일 경우 캘린더 이동
                    if (parsedMonth != null && parsedDay != null && parsedMonth in 1..12) {
                        try {
                            val parsedDate = LocalDate.of(parsedYear, parsedMonth!!, parsedDay!!)
                            selectedDate = parsedDate
                            currentMonth = YearMonth.from(parsedDate)

                            // 🌟 이동 완료 후 부드럽게 키보드 닫기
                            searchText = ""
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        } catch (e: Exception) {
                            // 2월 30일 같은 유효하지 않은 날짜는 무시
                        }
                    }
                },
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

            // 달력과 일정 리스트 사이 구분선
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

                        val baseTextColor = when (col) {
                            0 -> MaterialTheme.colorScheme.error
                            6 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else baseTextColor

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.4f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
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
                                if (hasEvent) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(modifier = Modifier
                                        .width(16.dp)
                                        .height(3.dp)
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
fun MonthViewHeader(
    searchText: String,
    onSearchChange: (String) -> Unit,
    onSearchExecute: () -> Unit,
    onAddEvent: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("캘린더", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            Surface(modifier = Modifier.size(36.dp).clickable { onAddEvent() }, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "추가", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = searchText,
            onValueChange = onSearchChange,
            placeholder = { Text("날짜 검색 (예: 11/11)", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), fontSize = 20.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
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
            singleLine = true,
            // 🌟 스마트폰 키보드의 '검색(돋보기)' 버튼을 감지하는 설정
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchExecute()
                }
            )
        )
    }
}

// 월 바꾸기 기능
@Composable
fun MonthNavigation(currentMonth: YearMonth, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
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
            val textColor = when (index) {
                0 -> MaterialTheme.colorScheme.error
                6 -> MaterialTheme.colorScheme.primary
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
                    // EventItem 컴포저블은 기존 프로젝트에 정의된 것을 사용합니다.
                    EventItem(event = event, onClick = onEventClick)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
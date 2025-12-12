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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MonthlyCalendarTab(
    events: List<Event>,
    onSave: (Event) -> Unit,
    onDelete: (Event) -> Unit
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
        sheetBackgroundColor = Color.White,
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
        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            MonthViewHeader(
                searchText = searchText,
                onSearchChange = { searchText = it },
                onAddEvent = {
                    editingEvent = null
                    scope.launch { modalSheetState.show() }
                }
            )

            MonthNavigation(
                currentMonth = currentMonth,
                onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
            )

            DaysOfWeekHeader()

            MonthGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                events = events,
                onDateSelected = { selectedDate = it }
            )

            Divider(color = Color(0xFFEEEEEE), thickness = 8.dp)

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
                            0 -> Color.Red
                            6 -> Color(0xFF4A80F0)
                            else -> Color.Black
                        }
                        val textColor = if (isSelected) Color.White else baseTextColor

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.4f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF4A80F0) else Color.Transparent)
                                .then(if (isToday && !isSelected) Modifier.border(1.5.dp, Color(0xFF4A80F0), RoundedCornerShape(10.dp)) else Modifier)
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(text = dayOfMonth.toString(), color = textColor, fontSize = 15.sp, fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal)
                                if (hasEvent) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(modifier = Modifier.width(16.dp).height(3.dp).background(if (isSelected) Color.White else Color(0xFF4A80F0), RoundedCornerShape(2.dp)))
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

@Composable
fun MonthViewHeader(searchText: String, onSearchChange: (String) -> Unit, onAddEvent: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF4A80F0)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("캘린더", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Surface(modifier = Modifier.size(36.dp).clickable { onAddEvent() }, shape = RoundedCornerShape(8.dp), color = Color.White) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "추가", tint = Color(0xFF4A80F0))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = searchText,
            onValueChange = onSearchChange,
            placeholder = { Text("날짜 검색 (예: 11/11)", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.White.copy(alpha = 0.2f), unfocusedContainerColor = Color.White.copy(alpha = 0.2f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = Color.White, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
    }
}

@Composable
fun MonthNavigation(currentMonth: YearMonth, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevMonth) { Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달") }
        val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월", Locale.KOREAN)
        Text(currentMonth.format(formatter), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        IconButton(onClick = onNextMonth) { Icon(Icons.Default.ChevronRight, contentDescription = "다음 달") }
    }
}

@Composable
fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val days = listOf("일", "월", "화", "수", "목", "금", "토")
        days.forEachIndexed { index, day ->
            val textColor = when (index) {
                0 -> Color.Red
                6 -> Color(0xFF4A80F0)
                else -> Color.Gray
            }
            Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = textColor, fontSize = 13.sp)
        }
    }
}

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
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(events) { event ->
                    EventItem(event = event, onClick = onEventClick)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
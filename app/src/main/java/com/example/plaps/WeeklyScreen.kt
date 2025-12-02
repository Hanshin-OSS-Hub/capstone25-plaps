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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WeeklyHomeScreen(
    events: List<Event>,
    onSave: (Event) -> Unit,
    onDelete: (Event) -> Unit
) {
    val modalSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentWeekStart by remember {
        mutableStateOf(LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value % 7.toLong()))
    }
    var editingEvent by remember { mutableStateOf<Event?>(null) }

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
        Column(modifier = Modifier.fillMaxSize()) {
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

@Composable
fun WeeklyCalendarHeader(currentWeekStart: LocalDate, selectedDate: LocalDate, events: List<Event>, onDateSelected: (LocalDate) -> Unit, onPrevWeek: () -> Unit, onNextWeek: () -> Unit, onAddEvent: () -> Unit) {
    val monthFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월")
    val weekFormatter = DateTimeFormatter.ofPattern("MM/dd")
    val weekStartStr = currentWeekStart.format(weekFormatter)
    val weekEndStr = currentWeekStart.plusDays(6).format(weekFormatter)

    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF4A80F0)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("내 캘린더", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(selectedDate.format(monthFormatter), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
            Surface(modifier = Modifier.size(40.dp).clickable { onAddEvent() }, shape = RoundedCornerShape(12.dp), color = Color.White) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "일정 추가", tint = Color(0xFF4A80F0))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevWeek) { Icon(Icons.Default.ChevronLeft, contentDescription = "이전 주", tint = Color.White) }
            Text("$weekStartStr - $weekEndStr", color = Color.White, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onNextWeek) { Icon(Icons.Default.ChevronRight, contentDescription = "다음 주", tint = Color.White) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            (0..6).forEach { i ->
                val date = currentWeekStart.plusDays(i.toLong())
                val hasEvent = events.any { it.date == date }
                WeeklyDayItem(date = date, isSelected = (date == selectedDate), hasEvent = hasEvent, onDateSelected = onDateSelected)
            }
        }
    }
}

@Composable
fun WeeklyDayItem(date: LocalDate, isSelected: Boolean, hasEvent: Boolean, onDateSelected: (LocalDate) -> Unit) {
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
    val dayOfMonth = date.dayOfMonth.toString()
    val backgroundColor = if (isSelected) Color.White else Color.Transparent
    val contentColor = if (isSelected) Color(0xFF4A80F0) else Color.White

    Column(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(backgroundColor).clickable { onDateSelected(date) }.padding(vertical = 8.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(dayOfWeek, color = contentColor.copy(alpha = 0.8f), fontSize = 12.sp)
        Text(dayOfMonth, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (hasEvent) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(contentColor))
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun WeeklyScheduleList(selectedDate: LocalDate, events: List<Event>, onEventClick: (Event) -> Unit) {
    val titleFormatter = DateTimeFormatter.ofPattern("MM월 dd일 EEEE", Locale.KOREAN)
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(selectedDate.format(titleFormatter), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("${events.size}개 일정", fontSize = 14.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (events.isEmpty()) {
            EmptyScheduleView()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(events) { event -> EventItem(event, onEventClick) }
            }
        }
    }
}
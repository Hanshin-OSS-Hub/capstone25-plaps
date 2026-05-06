package com.example.plaps

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plaps.data.Event
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventItem(event: Event, onClick: (Event) -> Unit) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val colors = listOf(Color(0xFF4A80F0), Color(0xFF4CAF50), Color(0xFFF44336), Color(0xFF9C27B0), Color(0xFFE91E63))
    val eventColor = colors.getOrElse(event.colorIndex) { colors[0] }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { onClick(event) }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.padding(top = 4.dp).size(10.dp).clip(CircleShape).background(eventColor))
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = "시간", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${event.startTime.format(timeFormatter)} - ${event.endTime.format(timeFormatter)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (event.location.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = "위치", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(event.location, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (event.latitude != null && event.longitude != null) {
                Button(
                    onClick = {
                        val intent = Intent(context, NaviLoadActivity::class.java).apply {
                            putExtra("DEST_NAME", event.location)
                            putExtra("DEST_LAT", event.latitude)
                            putExtra("DEST_LON", event.longitude)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp).padding(start = 8.dp)
                ) {
                    Text(text = "길찾기", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmptyScheduleView() {
    Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("일정이 없습니다", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text("새 일정을 추가해보세요", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditEventSheet(
    selectedDate: LocalDate,
    existingEvent: Event?,
    onClose: () -> Unit,
    onSave: (Event) -> Unit,
    onDelete: (Event) -> Unit
) {
    val focusManager = LocalFocusManager.current

    var title by remember(existingEvent) { mutableStateOf(existingEvent?.title ?: "") }
    var location by remember(existingEvent) { mutableStateOf(existingEvent?.location ?: "") }
    var latState by remember(existingEvent) { mutableStateOf(existingEvent?.latitude) }
    var lngState by remember(existingEvent) { mutableStateOf(existingEvent?.longitude) }
    var description by remember(existingEvent) { mutableStateOf(existingEvent?.notes ?: "") }
    var selectedColorIndex by remember(existingEvent) { mutableStateOf(existingEvent?.colorIndex ?: 0) }
    var startTime by remember(existingEvent) { mutableStateOf(existingEvent?.startTime ?: LocalTime.of(9, 0)) }
    var endTime by remember(existingEvent) { mutableStateOf(existingEvent?.endTime ?: LocalTime.of(10, 0)) }

    LaunchedEffect(existingEvent) {
        if (existingEvent == null) {
            title = ""; location = ""; latState = null; lngState = null; description = ""
            selectedColorIndex = 0; startTime = LocalTime.of(9, 0); endTime = LocalTime.of(10, 0)
        }
    }

    val colors = listOf(Color(0xFF4A80F0), Color(0xFF4CAF50), Color(0xFFF44336), Color(0xFF9C27B0), Color(0xFFE91E63))
    val inputBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val context = LocalContext.current
    val isEditMode = existingEvent != null
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            location = data.getStringExtra("result_place_name") ?: ""
            latState = data.getDoubleExtra("result_lat", 0.0).takeIf { it != 0.0 }
            lngState = data.getDoubleExtra("result_lng", 0.0).takeIf { it != 0.0 }
        }
    }

    val showTimePicker = { isStartTime: Boolean ->
        val initialTime = if (isStartTime) startTime else endTime
        val dialog = TimePickerDialog(
            context,
            android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            { _, hour, minute ->
                val selectedTime = LocalTime.of(hour, minute)
                if (isStartTime) {
                    startTime = selectedTime
                    if (startTime.isAfter(endTime) || startTime == endTime) endTime = startTime.plusHours(1)
                } else {
                    if (selectedTime.isBefore(startTime) || selectedTime == startTime) {
                        Toast.makeText(context, "종료 시간을 확인해주세요.", Toast.LENGTH_SHORT).show()
                    } else endTime = selectedTime
                }
            },
            initialTime.hour,
            initialTime.minute,
            true
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 700.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .imePadding()
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (isEditMode) "일정 수정" else "새 일정 추가", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Row {
                if (isEditMode) {
                    IconButton(onClick = { onDelete(existingEvent!!) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "닫기", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text("제목 *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            TextField(
                value = title, onValueChange = { title = it }, placeholder = { Text("일정 제목", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = inputBackgroundColor, unfocusedContainerColor = inputBackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(8.dp), singleLine = true, textStyle = TextStyle(fontSize = 14.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("날짜 *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(inputBackgroundColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(selectedDate.toString(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f).clickable { showTimePicker(true) }) {
                    Text("시작 시간", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(inputBackgroundColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(startTime.format(timeFormatter), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f).clickable { showTimePicker(false) }) {
                    Text("종료 시간", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(inputBackgroundColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(endTime.format(timeFormatter), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("색상", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEachIndexed { index, color ->
                    val isSelected = (selectedColorIndex == index)
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).clickable { selectedColorIndex = index }
                        .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("설명", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            TextField(
                value = description, onValueChange = { description = it }, placeholder = { Text("일정 설명", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().height(70.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = inputBackgroundColor, unfocusedContainerColor = inputBackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(8.dp), textStyle = TextStyle(fontSize = 14.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("위치", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().clickable {
                val intent = Intent(context, LocationActivity::class.java)
                locationLauncher.launch(intent)
            }) {
                TextField(
                    value = location, onValueChange = {}, placeholder = { Text("터치하여 장소 검색", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().height(80.dp), readOnly = true, enabled = false,
                    colors = TextFieldDefaults.colors(disabledContainerColor = inputBackgroundColor, disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledIndicatorColor = Color.Transparent),
                    shape = RoundedCornerShape(8.dp), textStyle = TextStyle(fontSize = 14.sp),
                    trailingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Column {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
                    } else {
                        focusManager.clearFocus()
                        onSave(Event(id = existingEvent?.id ?: 0, date = selectedDate, title = title, startTime = startTime, endTime = endTime, location = location, notes = description, colorIndex = selectedColorIndex, latitude = latState, longitude = lngState))
                        title = ""; location = ""; latState = null; lngState = null; description = ""
                        selectedColorIndex = 0; startTime = LocalTime.of(9, 0); endTime = LocalTime.of(10, 0)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(45.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isEditMode) "수정 완료" else "저장", color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onClose()
                },
                modifier = Modifier.fillMaxWidth().height(45.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("취소", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
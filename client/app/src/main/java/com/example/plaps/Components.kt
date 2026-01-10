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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { onClick(event) }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // 1. 왼쪽 색상 원
            Box(modifier = Modifier.padding(top = 4.dp).size(10.dp).clip(CircleShape).background(eventColor))
            Spacer(modifier = Modifier.width(12.dp))

            // 2. 가운데 텍스트 영역
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = "시간", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${event.startTime.format(timeFormatter)} - ${event.endTime.format(timeFormatter)}", fontSize = 14.sp, color = Color.Gray)
                }
                if (event.location.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = "위치", modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(event.location, fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
            //길찾기 버튼에 데이터 전달 로직 추가
            //3. 오른쪽 길찾기 버튼 (좌표 정보가 있을 때만 표시)
            // event.latitude, longitude가 null이 아닐 때만 버튼이 보입니다.
            if (event.latitude != null && event.longitude != null) {
                Button(
                    onClick = {
                        // ★ [수정 핵심] Intent에 장소 이름, 위도, 경도를 담아서 보냄
                        val intent = Intent(context, NaviLoadActivity::class.java).apply {
                            putExtra("DEST_NAME", event.location)
                            putExtra("DEST_LAT", event.latitude)  // Double
                            putExtra("DEST_LON", event.longitude) // Double
                        }
                        context.startActivity(intent)
                        Toast.makeText(context, "길찾기 기능 (NaviLoadActivity) 준비 중", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A80F0)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp).padding(start = 8.dp)
                ) {
                    Text(
                        text = "길찾기",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyScheduleView() {
    Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.Gray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("일정이 없습니다", fontSize = 18.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text("새 일정을 추가해보세요", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
    }
}

// [추가 수정] AddOrEditEventSheet: 좌표 저장 로직 추가
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditEventSheet(
    selectedDate: LocalDate,
    existingEvent: Event?,
    onClose: () -> Unit,
    onSave: (Event) -> Unit,
    onDelete: (Event) -> Unit
) {
    var title by remember(existingEvent) { mutableStateOf(existingEvent?.title ?: "") }
    var location by remember(existingEvent) { mutableStateOf(existingEvent?.location ?: "") }
    // 👇 위도, 경도 변수를 remember 상태로 추가
    var latitude by remember(existingEvent) { mutableStateOf(existingEvent?.latitude ?: 0.0) }
    var longitude by remember(existingEvent) { mutableStateOf(existingEvent?.longitude ?: 0.0) }

    var description by remember(existingEvent) { mutableStateOf(existingEvent?.notes ?: "") }
    var selectedColorIndex by remember(existingEvent) { mutableStateOf(existingEvent?.colorIndex ?: 0) }
    // 👇 시간 관련 상태 변수 추가
    var startTime by remember(existingEvent) { mutableStateOf(existingEvent?.startTime ?: LocalTime.of(9, 0)) }
    var endTime by remember(existingEvent) { mutableStateOf(existingEvent?.endTime ?: LocalTime.of(10, 0)) }

    //  좌표를 저장할 상태 변수 (기존 값 있으면 불러오고, 없으면 null)
    var latitude by remember(existingEvent) { mutableStateOf(existingEvent?.latitude) }
    var longitude by remember(existingEvent) { mutableStateOf(existingEvent?.longitude) }

    val colors = listOf(Color(0xFF4A80F0), Color(0xFF4CAF50), Color(0xFFF44336), Color(0xFF9C27B0), Color(0xFFE91E63))
    val inputBackgroundColor = Color(0xFFF3F4F6)
    val context = LocalContext.current
    val isEditMode = existingEvent != null
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // 위치 검색 결과를 받는 Launcher (LocationActivity에서 결과 받아오기)
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                // 장소 이름 가져오기
                val placeName = data.getStringExtra("result_place_name")
                if (placeName != null) location = placeName

                // 좌표 가져오기 (LocationActivity에서 이 이름으로 보내줘야 함)
                val lat = data.getDoubleExtra("result_lat", 0.0)
                val lon = data.getDoubleExtra("result_lng", 0.0)

                // 0.0이 아니면 상태 변수에 저장
                if (lat != 0.0 && lon != 0.0) {
                    latitude = lat
                    longitude = lon
                }
            }
        }
    }
    // 👇 TimePickerDialog를 띄워주는 함수 정의 (시작/종료 시간 유효성 검사 포함)
    val showTimePicker = { isStartTime: Boolean ->
        val initialTime = if (isStartTime) startTime else endTime
        TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                val selectedTime = LocalTime.of(hour, minute)
                if (isStartTime) {
                    startTime = selectedTime
                    // 시작 시간이 종료 시간보다 늦다면, 종료 시간도 1시간 뒤로 조정
                    if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
                        endTime = startTime.plusHours(1).withMinute(minute)
                    }
                } else {
                    // 종료 시간이 시작 시간보다 빠르거나 같다면, 경고 메시지를 띄우고 시간을 변경하지 않음
                    if (selectedTime.isBefore(startTime) || selectedTime.equals(startTime)) {
                        Toast.makeText(context, "종료 시간이 시작 시간보다 빠르거나 같을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        endTime = selectedTime
                    }
                }
            },
            initialTime.hour, // 초기 시간 (시)
            initialTime.minute, // 초기 시간 (분)
            true // 24시간 형식 사용
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (isEditMode) "일정 수정" else "새 일정 추가", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row {
                if (isEditMode) {
                    IconButton(onClick = { onDelete(existingEvent!!) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.Red)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "닫기")
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // 제목 입력
        Text("제목 *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        TextField(value = title, onValueChange = { title = it }, placeholder = { Text("일정 제목", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = TextFieldDefaults.colors(focusedContainerColor = inputBackgroundColor, unfocusedContainerColor = inputBackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(8.dp), singleLine = true, textStyle = TextStyle(fontSize = 14.sp))

        Spacer(modifier = Modifier.height(12.dp))

        // 날짜 표시
        Text("날짜 *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(inputBackgroundColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(selectedDate.toString(), fontSize = 14.sp)
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 👇 시간 입력 (클릭 가능하도록 수정)
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f).clickable { showTimePicker(true) }) {
                Text("시작 시간", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(inputBackgroundColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(startTime.format(timeFormatter), fontSize = 14.sp, color = Color.Black) // 👈 상태 변수 값 표시
                    Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            // [B] 종료 시간 설정 - clickable 추가
            Column(modifier = Modifier.weight(1f).clickable { showTimePicker(false) }) {
                Text("종료 시간", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(inputBackgroundColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(endTime.format(timeFormatter), fontSize = 14.sp, color = Color.Black) // 👈 상태 변수 값 표시
                    Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                }
            }
        }
        // 👆 시간 입력 (클릭 가능하도록 수정)
        Spacer(modifier = Modifier.height(12.dp))

        // 색상 선택
        Text("색상", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEachIndexed { index, color ->
                val isSelected = (selectedColorIndex == index)
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).clickable { selectedColorIndex = index }.then(if (isSelected) Modifier.border(2.dp, Color.Black.copy(alpha = 0.5f), CircleShape) else Modifier))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 설명 입력
        Text("설명", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        TextField(value = description, onValueChange = { description = it }, placeholder = { Text("일정 설명", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth().height(70.dp), colors = TextFieldDefaults.colors(focusedContainerColor = inputBackgroundColor, unfocusedContainerColor = inputBackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(8.dp), textStyle = TextStyle(fontSize = 14.sp))

        Spacer(modifier = Modifier.height(12.dp))

        // 위치 입력 (LocationActivity 호출 로직 유지)
        Text("위치", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = location,
                onValueChange = {}, // 입력 막음
                placeholder = { Text("터치하여 장소 검색", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = inputBackgroundColor, unfocusedContainerColor = inputBackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledContainerColor = inputBackgroundColor),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp),
                readOnly = true, // 키보드 안 올라오게 설정
                trailingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color.Gray) }
            )
            // 투명한 클릭 영역 (LocationActivity 실행)
            Box(
                modifier = Modifier.matchParentSize().clickable {
                    val intent = Intent(context, LocationActivity::class.java)
                    locationLauncher.launch(intent)
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 저장 버튼
        Button(onClick = {
            // [추가된 로직] 시작 시간이 종료 시간보다 늦은지 확인
            if (startTime.isAfter(endTime)) {
                Toast.makeText(context, "종료 시간은 시작 시간보다 늦어야 합니다.", Toast.LENGTH_SHORT).show()
                return@Button // 저장을 하지 않고 함수를 빠져나갑니다.
            }
            if (title.isBlank()) {
                Toast.makeText(context, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
            } else {
                val eventToSave = Event(
                    id = existingEvent?.id ?: 0,
                    date = selectedDate,
                    title = title,
                    startTime = startTime, // 👈 수정된 상태 변수 사용
                    endTime = endTime,     // 👈 수정된 상태 변수 사용
                    location = location,
                    // 장소 정보가 있을 때만 좌표값 저장, 없으면 0.0
                    latitude = if (location.isNotBlank()) (latitude ?: 0.0) else 0.0,
                    longitude = if (location.isNotBlank()) (longitude ?: 0.0) else 0.0,
                    notes = description,
                    colorIndex = selectedColorIndex,
                    // 받아온 좌표를 저장
                    latitude = latitude,
                    longitude = longitude
                )
                onSave(eventToSave)
            }
        }, modifier = Modifier.fillMaxWidth().height(45.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black), shape = RoundedCornerShape(8.dp)) {
            Text(if (isEditMode) "수정 완료" else "저장", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(45.dp).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)), colors = ButtonDefaults.buttonColors(containerColor = Color.White), elevation = ButtonDefaults.buttonElevation(0.dp), shape = RoundedCornerShape(8.dp)) {
            Text("취소", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
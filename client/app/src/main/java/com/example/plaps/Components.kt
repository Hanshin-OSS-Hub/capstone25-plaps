package com.example.plaps

import android.widget.Toast
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import android.app.Activity // [추가] 결과 코드(RESULT_OK) 확인용
import android.content.Intent // [추가] Intent 사용용
import androidx.activity.compose.rememberLauncherForActivityResult // [추가] Launcher
import androidx.activity.result.contract.ActivityResultContracts // [추가] Contract
import androidx.compose.ui.platform.LocalContext // [추가]
import androidx.compose.material.icons.filled.Place // [추가] 또는 원하는 아이콘
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventItem(event: Event, onClick: (Event) -> Unit) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val colors = listOf(Color(0xFF4A80F0), Color(0xFF4CAF50), Color(0xFFF44336), Color(0xFF9C27B0), Color(0xFFE91E63))
    val eventColor = colors.getOrElse(event.colorIndex) { colors[0] }
    val context = LocalContext.current // [추가] 화면 이동용 Context

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { onClick(event) }
    ) {
        Row(modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically // [추가] 수직 중앙 정렬
            ) {
            // 1. 왼쪽 색상 원 (기존 코드)
            Box(modifier = Modifier.padding(top = 4.dp).size(10.dp).clip(CircleShape).background(eventColor))
            Spacer(modifier = Modifier.width(12.dp))

            // 2. 가운데 텍스트 영역 (수정됨!)
            // [추가] modifier.weight(1f)를 추가해야 버튼이 오른쪽 끝으로 갑니다.
            // Column {} 에서 Column(modifier = Modifier.weight(1f)) {}로 수정됨.
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

            // 3. 오른쪽 길찾기 버튼 (수정됨: 아이콘 -> 텍스트 버튼)
            if (event.location.isNotBlank()) {
                Button(
                    onClick = {
                        val intent = Intent(context, NaviLoadActivity::class.java)
                        context.startActivity(intent)
                    },
                    // 버튼 스타일링: 파란색 배경, 둥근 모서리, 내용물 패딩 조절
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A80F0)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp) // 버튼 높이를 너무 크지 않게 조절
                        .padding(start = 8.dp) // 텍스트와 간격 벌리기
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
    var description by remember(existingEvent) { mutableStateOf(existingEvent?.notes ?: "") }
    var selectedColorIndex by remember(existingEvent) { mutableStateOf(existingEvent?.colorIndex ?: 0) }

    val colors = listOf(Color(0xFF4A80F0), Color(0xFF4CAF50), Color(0xFFF44336), Color(0xFF9C27B0), Color(0xFFE91E63))
    val inputBackgroundColor = Color(0xFFF3F4F6)
    val context = LocalContext.current
    val isEditMode = existingEvent != null

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // LocationActivity에서 "result_place_name"라는 이름으로 보냈다고 가정
            val placeName = result.data?.getStringExtra("result_place_name")
            if (placeName != null) {
                location = placeName // 받아온 값으로 위치 변수 업데이트!
            }
        }
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

        // 시간 입력 (더미 UI)
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("시작 시간", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(inputBackgroundColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("09:00", fontSize = 14.sp, color = Color.Gray)
                    Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("종료 시간", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(inputBackgroundColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("10:00", fontSize = 14.sp, color = Color.Gray)
                    Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                }
            }
        }

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

        // 설명 및 위치
        Text("설명", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        TextField(value = description, onValueChange = { description = it }, placeholder = { Text("일정 설명", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth().height(70.dp), colors = TextFieldDefaults.colors(focusedContainerColor = inputBackgroundColor, unfocusedContainerColor = inputBackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(8.dp), textStyle = TextStyle(fontSize = 14.sp))

        Spacer(modifier = Modifier.height(12.dp))

        // ▼▼▼ [여기서부터 기존 코드를 지우고 붙여넣으세요] ▼▼▼
        // [2] 변경된 부분: 위치 입력 칸 수정
        Text("위치", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))

        // 클릭 이벤트를 위해 Box로 감쌈
        Box(modifier = Modifier.fillMaxWidth()) {
            // 1. 화면에 보이는 입력창 (모양 담당)
            TextField(
                value = location,
                onValueChange = {}, // 입력 막음
                placeholder = { Text("터치하여 장소 검색", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF3F4F6),
                    unfocusedContainerColor = Color(0xFFF3F4F6),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledContainerColor = Color(0xFFF3F4F6)
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp),
                readOnly = true, // 키보드 안 올라오게 설정
                trailingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color.Gray) }
            )

            // 2. 투명한 클릭 영역 (기능 담당) - 여기가 핵심입니다!
            // matchParentSize()로 입력창과 똑같은 크기의 투명한 막을 위에 덮습니다.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable {
                        // 입력창 아무데나 눌러도 실행됨
                        val intent = Intent(context, LocationActivity::class.java)
                        locationLauncher.launch(intent)
                    }
            )
        }
        // ▲▲▲ [여기까지 붙여넣기] ▲▲▲

        Spacer(modifier = Modifier.height(20.dp))

        // 버튼
        Button(onClick = {
            if (title.isBlank()) {
                Toast.makeText(context, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
            } else {
                val eventToSave = Event(
                    id = existingEvent?.id ?: 0,
                    date = selectedDate,
                    title = title,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(10, 0),
                    location = location,
                    notes = description,
                    colorIndex = selectedColorIndex
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
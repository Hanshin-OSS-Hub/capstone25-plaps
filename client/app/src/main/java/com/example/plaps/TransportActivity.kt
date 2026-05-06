package com.example.plaps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plaps.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 길찾기 버튼에서 넘겨준 목적지 정보 받기
        val destLat = intent.getDoubleExtra("DEST_LAT", 0.0)
        val destLon = intent.getDoubleExtra("DEST_LON", 0.0)
        val destName = intent.getStringExtra("DEST_NAME") ?: "목적지"

        setContent {
            TransportRouteScreen(this, destLat, destLon, destName)
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportRouteScreen(context: Context, destLat: Double, destLon: Double, destName: String) {
    val coroutineScope = rememberCoroutineScope()
    var routeList by remember { mutableStateOf<List<com.example.plaps.data.PathItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // 1. 내 현재 위치 가져오기
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                // 2. 시작 좌표(내 위치)
                val startX = myLocation?.longitude?.toString()
                val startY = myLocation?.latitude?.toString()

                // 3. 내 위치를 못 찾았을 때
                if (startX == null || startY == null) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "현재 위치를 찾을 수 없습니다."
                        isLoading = false
                    }
                    return@launch
                }

                // 4. 목적지 좌표
                val endX = destLon.toString()
                val endY = destLat.toString()

                // 5. 목적지 좌표 예외 처리
                if (endX == "0.0" || endY == "0.0") {
                    withContext(Dispatchers.Main) {
                        errorMessage = "목적지 좌표가 유효하지 않습니다."
                        isLoading = false
                    }
                    return@launch
                }

                // 6. API 호출
                val myApiKey = BuildConfig.ODSAY_API_KEY
                val response = RetrofitClient.odsayService.getTransitPath(
                    myApiKey, startX, startY, endX, endY
                )

                // 7. 결과 및 에러 화면 처리
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val paths = body?.result?.pathList
                        if (!paths.isNullOrEmpty()) {
                            routeList = paths
                        } else {
                            errorMessage = "해당 목적지로 가는 대중교통 경로가 없습니다."
                        }
                    } else {
                        errorMessage = "서버 통신 실패 (코드: ${response.code()})"
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "네트워크 에러: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "$destName 가는 길", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF0F0F0))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(routeList) { path ->
                        // 버튼 눌렀을 때의 동작
                        RouteCard(path = path, onNavigateClick = { mapObj ->
                            if (mapObj != null) {
                                // TransPathActivity로 지도 데이터 intent로 들고 가기
                                val intent = Intent(context, TransPathActivity::class.java).apply {
                                    putExtra("MAP_OBJ", mapObj)
                                    putExtra("DEST_LAT", destLat)
                                    putExtra("DEST_LON", destLon)
                                }
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "경로 데이터가 없어 지도를 띄울 수 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }
        }
    }
}
@Composable
fun RouteCard(path: com.example.plaps.data.PathItem, onNavigateClick: (String?) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. 총 소요 시간과 요금
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${path.info.totalTime}분",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${path.info.payment}원",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. 타임라인 바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0))
            ) {
                val totalTime = path.info.totalTime.toFloat()
                if (totalTime > 0f) {
                    path.subPathList.forEach { sub ->
                        val minWeight = if (sub.sectionTime > 0) 0.08f else 0.02f
                        val weight = (sub.sectionTime.toFloat() / totalTime).coerceAtLeast(minWeight)

                        val color = when (sub.trafficType) {
                            1 -> Color(0xFF2B50A1)
                            2 -> Color(0xFF4CAF50)
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxHeight()
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            // 0분일 때는 빈 칸만 남기고 글씨는 숨기기
                            if (sub.sectionTime > 0) {
                                Text(
                                    text = "${sub.sectionTime}분",
                                    color = if (sub.trafficType == 3) Color.DarkGray else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 상세 환승 정보
            path.subPathList.forEach { sub ->
                if (sub.trafficType == 1 || sub.trafficType == 2) {
                    val isSubway = sub.trafficType == 1

                    val transportName = if (isSubway) {
                        sub.laneList?.getOrNull(0)?.name ?: "지하철"
                    } else {
                        sub.laneList?.getOrNull(0)?.busNo?.let { "$it 번 버스" } ?: "버스"
                    }

                    val transportColor = if (isSubway) Color(0xFF2B50A1) else Color(0xFF4CAF50)

                    Row(
                        modifier = Modifier
                            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(transportColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSubway) "지" else "버",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "${sub.startName ?: "출발지"} 승차",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "[$transportName] 이동 (${sub.sectionTime}분)",
                                fontSize = 13.sp,
                                color = transportColor,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${sub.endName ?: "도착지"} 하차",
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            // 4. 안내시작 버튼
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onNavigateClick(path.info.mapObj) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Text(text = "▲ 안내시작", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
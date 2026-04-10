package com.example.plaps

import android.annotation.SuppressLint
import android.content.Context
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
import com.example.plaps.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.sp

class TransportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 길찾기 버튼에서 넘겨준 목적지 정보 받기
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
    // 경로 리스트를 담을 상태 (임시로 Any 타입 사용, 실제 PathItem 클래스에 맞게 수정 필요할 수 있음)
    var routeList by remember { mutableStateOf<List<com.example.plaps.data.PathItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // 내 현재 위치(GPS) 가져오기
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                val startX = myLocation?.longitude?.toString() ?: "126.9708"
                val startY = myLocation?.latitude?.toString() ?: "37.2999"
                val endX = destLon.toString()
                val endY = destLat.toString()

                if (endX == "0.0" || endY == "0.0") {
                    errorMessage = "목적지 좌표가 유효하지 않습니다."
                    isLoading = false
                    return@launch
                }

                val myApiKey = BuildConfig.ODSAY_API_KEY
                val response = RetrofitClient.odsayService.getTransitPath(
                    myApiKey, startX, startY, endX, endY
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val paths = body?.result?.pathList
                        if (!paths.isNullOrEmpty()) {
                            routeList = paths
                        } else {
                            errorMessage = "해당 목적지로 가는 대중교통 경로가 없습니다."
                        }
                    }
                    else {
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
                .background(Color(0xFFF0F0F0)) // 전체 배경 연한 회색
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
                        RouteCard(path) // 아래에서 만든 카드로 하나씩 그리기
                    }
                }
            }
        }
    }
}

// 경로 하나를 예쁘게 카드로 그려주는 함수
@Composable
fun RouteCard(path: com.example.plaps.data.PathItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. 총 소요 시간과 요금
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${path.info.totalTime}분",
                    fontSize = 24.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${path.info.payment}원",
                    fontSize = 14.sp,
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. 타임라인 바 (0분짜리는 얇게, 나머지는 정상적으로!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFFE0E0E0))
            ) {
                val totalTime = path.info.totalTime.toFloat()
                if (totalTime > 0f) {
                    path.subPathList.forEach { sub ->
                        // 💡 핵심: 0분짜리는 2%의 얇은 틈새만 주고, 1분 이상은 8% 이상의 칸을 줍니다!
                        val minWeight = if (sub.sectionTime > 0) 0.08f else 0.02f
                        val weight = (sub.sectionTime.toFloat() / totalTime).coerceAtLeast(minWeight)

                        val color = when (sub.trafficType) {
                            1 -> androidx.compose.ui.graphics.Color(0xFF2B50A1)
                            2 -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            else -> androidx.compose.ui.graphics.Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxHeight()
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            // 0분일 때는 빈 칸만 남기고 글씨는 숨깁니다
                            if (sub.sectionTime > 0) {
                                Text(
                                    text = "${sub.sectionTime}분",
                                    color = if (sub.trafficType == 3) androidx.compose.ui.graphics.Color.DarkGray else androidx.compose.ui.graphics.Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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

                    val transportColor = if (isSubway) androidx.compose.ui.graphics.Color(0xFF2B50A1) else androidx.compose.ui.graphics.Color(0xFF4CAF50)

                    Row(
                        modifier = Modifier
                            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(24.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                .background(transportColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSubway) "지" else "버",
                                color = androidx.compose.ui.graphics.Color.White,
                                fontSize = 11.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "${sub.startName ?: "출발지"} 승차",
                                fontSize = 15.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.Black
                            )
                            Text(
                                text = "[$transportName] 이동 (${sub.sectionTime}분)",
                                fontSize = 13.sp,
                                color = transportColor,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            Text(
                                text = "${sub.endName ?: "도착지"} 하차",
                                fontSize = 14.sp,
                                color = androidx.compose.ui.graphics.Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}
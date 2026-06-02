package com.example.plaps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
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
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                val startX = myLocation?.longitude?.toString()
                val startY = myLocation?.latitude?.toString()

                if (startX == null || startY == null) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "현재 위치를 찾을 수 없습니다."
                        isLoading = false
                    }
                    return@launch
                }

                val endX = destLon.toString()
                val endY = destLat.toString()

                if (endX == "0.0" || endY == "0.0") {
                    withContext(Dispatchers.Main) {
                        errorMessage = "목적지 좌표가 유효하지 않습니다."
                        isLoading = false
                    }
                    return@launch
                }

                val myApiKey = BuildConfig.ODSAY_API_KEY
                val response = RetrofitClient.odsayService.getTransitPath(
                    myApiKey, startX, startY, endX, endY
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val paths = body?.result?.pathList

                    if (!paths.isNullOrEmpty()) {
                        val isStationOrTerminal = destName.endsWith("역") || destName.endsWith("터미널")
                        val exactPaths = if (isStationOrTerminal) {
                            val keyword = destName.replace("역", "").replace("터미널", "").trim()
                            paths.filter { path ->
                                val lastTransit = path.subPathList.findLast { it.trafficType != 3 }
                                val endName = lastTransit?.endName ?: ""
                                endName.contains(keyword)
                            }
                        } else {
                            paths
                        }

                        if (exactPaths.isNotEmpty()) {
                            val finalRouteList = mutableListOf<com.example.plaps.data.PathItem>()

                            val isLongDistance = exactPaths.any { path ->
                                path.subPathList.any { sub -> sub.trafficType in listOf(4, 5, 6) }
                            }

                            val pathLimit = if (isLongDistance) 5 else 10
                            val topPaths = exactPaths.take(pathLimit)

                            for (path in topPaths) {
                                val trunkSubPaths = path.subPathList.filter { it.trafficType in listOf(4, 5, 6) }

                                if (trunkSubPaths.isNotEmpty()) {
                                    val firstTrunk = trunkSubPaths.first()
                                    val lastTrunk = trunkSubPaths.last()

                                    val stationBX = firstTrunk.startX?.toString()
                                    val stationBY = firstTrunk.startY?.toString()
                                    val stationCX = lastTrunk.endX?.toString()
                                    val stationCY = lastTrunk.endY?.toString()

                                    if (stationBX != null && stationBY != null && stationCX != null && stationCY != null) {
                                        val firstMileDef = async { RetrofitClient.odsayService.getTransitPath(myApiKey, startX, startY, stationBX, stationBY) }
                                        val lastMileDef = async { RetrofitClient.odsayService.getTransitPath(myApiKey, stationCX, stationCY, endX, endY) }

                                        val firstResp = try { firstMileDef.await() } catch (e: Exception) { null }
                                        val lastResp = try { lastMileDef.await() } catch (e: Exception) { null }

                                        val firstBest = firstResp?.body()?.result?.pathList?.firstOrNull()
                                        val lastBest = lastResp?.body()?.result?.pathList?.firstOrNull()

                                        val mergedTotalTime = (firstBest?.info?.totalTime ?: 0) + path.info.totalTime + (lastBest?.info?.totalTime ?: 0)
                                        val mergedPayment = (firstBest?.info?.payment ?: 0) + path.info.payment + (lastBest?.info?.payment ?: 0)

                                        val mapObjs = listOfNotNull(firstBest?.info?.mapObj, lastBest?.info?.mapObj).filter { it.isNotBlank() }
                                        val mergedMapObj = if (mapObjs.isNotEmpty()) mapObjs.joinToString("@") else null

                                        val mergedSubPaths = mutableListOf<com.example.plaps.data.SubPathItem>()
                                        if (firstBest != null) mergedSubPaths.addAll(firstBest.subPathList)
                                        mergedSubPaths.addAll(trunkSubPaths)
                                        if (lastBest != null) mergedSubPaths.addAll(lastBest.subPathList)

                                        val mergedInfo = path.info.copy(totalTime = mergedTotalTime, payment = mergedPayment, mapObj = mergedMapObj)
                                        finalRouteList.add(path.copy(info = mergedInfo, subPathList = mergedSubPaths))
                                    } else {
                                        finalRouteList.add(path)
                                    }
                                } else {
                                    finalRouteList.add(path)
                                }
                            }

                            withContext(Dispatchers.Main) {
                                routeList = finalRouteList
                                isLoading = false
                            }

                        } else {
                            withContext(Dispatchers.Main) {
                                errorMessage = "목적지까지 한 번에 가는 정확한 경로를 찾을 수 없습니다."
                                isLoading = false
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            errorMessage = "해당 목적지로 가는 대중교통 경로가 없습니다."
                            isLoading = false
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        errorMessage = "서버 통신 실패 (코드: ${response.code()})"
                        isLoading = false
                    }
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
                        RouteCard(path = path, onNavigateClick = { pathObj ->
                            val gson = Gson()
                            val pathJson = gson.toJson(pathObj)

                            val intent = Intent(context, TransPathActivity::class.java).apply {
                                putExtra("PATH_JSON", pathJson)
                                putExtra("DEST_LAT", destLat)
                                putExtra("DEST_LON", destLon)
                            }
                            context.startActivity(intent)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun RouteCard(path: com.example.plaps.data.PathItem, onNavigateClick: (com.example.plaps.data.PathItem) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            val hasTrunkRoute = path.subPathList.any { it.trafficType in listOf(4, 5, 6) }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${path.info.totalTime}분",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (hasTrunkRoute) "${path.info.payment}원 + α" else "${path.info.payment}원",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                if (hasTrunkRoute) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(광역수단 요금 제외)",
                        fontSize = 11.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                            4 -> Color(0xFFE74C3C)
                            5, 6 -> Color(0xFF8E44AD)
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxHeight()
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
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

            path.subPathList.forEach { sub ->
                if (sub.trafficType in listOf(1, 2, 4, 5, 6)) {
                    val isSubway = sub.trafficType == 1
                    val isBus = sub.trafficType == 2
                    val isTrain = sub.trafficType == 4

                    val transportName = when {
                        isSubway -> sub.laneList?.getOrNull(0)?.name ?: "지하철"
                        isBus -> sub.laneList?.getOrNull(0)?.busNo?.let { "$it 번 버스" } ?: "버스"
                        isTrain -> sub.laneList?.getOrNull(0)?.name ?: "기차/KTX"
                        else -> sub.laneList?.getOrNull(0)?.name ?: "시외/고속버스"
                    }

                    val transportColor = when {
                        isSubway -> Color(0xFF2B50A1)
                        isBus -> Color(0xFF4CAF50)
                        isTrain -> Color(0xFFE74C3C)
                        else -> Color(0xFF8E44AD)
                    }

                    val iconText = when {
                        isSubway -> "지"
                        isBus -> "버"
                        isTrain -> "기"
                        else -> "시"
                    }

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
                                text = iconText,
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

                            // ~역 방면 문구
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "[$transportName] 이동 (${sub.sectionTime}분)",
                                    fontSize = 13.sp,
                                    color = transportColor,
                                    fontWeight = FontWeight.Medium
                                )

                                val nextStation = sub.passStopList?.stations?.getOrNull(1)?.stationName
                                val directionText = when {
                                    !nextStation.isNullOrBlank() -> "($nextStation 방면)"
                                    !sub.way.isNullOrBlank() -> "(${sub.way} 방면)"
                                    else -> null
                                }

                                if (directionText != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = directionText,
                                        fontSize = 12.sp,
                                        color = Color(0xFFE74C3C),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "${sub.endName ?: "도착지"} 하차",
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onNavigateClick(path) },
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
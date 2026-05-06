package com.example.plaps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.plaps.api.RetrofitClient
import com.example.plaps.data.ODsayLaneResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextStyle
import com.kakao.vectormap.route.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.kakao.vectormap.label.LabelTextBuilder

class TransPathActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapObj = intent.getStringExtra("MAP_OBJ") ?: ""
        val destLat = intent.getDoubleExtra("DEST_LAT", 0.0)
        val destLon = intent.getDoubleExtra("DEST_LON", 0.0)

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                TransPathScreen(fusedLocationClient, mapObj, destLat, destLon)
            }
        }
    }
}

@Composable
fun TransPathScreen(
    fusedLocationClient: FusedLocationProviderClient,
    mapObj: String,
    destLat: Double,
    destLon: Double
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var laneData by remember { mutableStateOf<ODsayLaneResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var kakaoMapInstance by remember { mutableStateOf<KakaoMap?>(null) }

    // 내 현재 위치 좌표를 저장할 State (출발지점 도보 연결을 위해 필요)
    var myLocation by remember { mutableStateOf<LatLng?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(context, "위치 권한이 승인되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    val moveToMyLocation = { map: KakaoMap ->
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val position = LatLng.from(location.latitude, location.longitude)
                        myLocation = position // 상태 저장

                        map.moveCamera(CameraUpdateFactory.newCenterPosition(position, 17))

                        val labelManager = map.labelManager
                        var layer = labelManager?.getLayer("my_location_layer")
                        if (layer == null) {
                            layer = labelManager?.addLayer(LabelLayerOptions.from("my_location_layer"))
                        }

                        val style = LabelStyle.from(android.R.drawable.btn_star_big_on).setZoomLevel(0)
                        val styles = labelManager?.addLabelStyles(LabelStyles.from(style))

                        val myLabel = layer?.getLabel("my_position")
                        if (myLabel != null) {
                            myLabel.moveTo(position)
                        } else {
                            layer?.addLabel(LabelOptions.from("my_position", position).setStyles(styles))
                        }
                    }
                }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // 도보 구간(회색 선)과 시간 텍스트를 그려주는 핵심 함수
    fun drawWalkingSection(map: KakaoMap, startPoint: LatLng, endPoint: LatLng, id: String) {
        // 1. 실제 거리(m) 계산
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            startPoint.latitude, startPoint.longitude,
            endPoint.latitude, endPoint.longitude,
            results
        )
        val distance = results[0]
        if (distance < 20) return // 거리가 너무 짧으면(20m 이하) 그리지 않음

        // 2. 도보 소요 시간 계산 (성인 평균 걸음걸이 분당 67m 기준)
        val walkTime = Math.ceil((distance / 67.0).toDouble()).toInt()

        // 3. 도보 선 그리기 (회색 선)
        val layer = map.routeLineManager?.layer
        val points = listOf(startPoint, endPoint)

        val style = RouteLineStyle.from(8f, android.graphics.Color.parseColor("#9E9E9E"))
        val styles = RouteLineStyles.from(style)
        val styleSet = RouteLineStylesSet.from("walk_style_$id", styles)
        val segment = RouteLineSegment.from(points).setStyles(styleSet.getStyles(0))
        layer?.addRouteLine(RouteLineOptions.from(segment).setStylesSet(styleSet))

        // 4. 선 중앙에 '약 ~분' 텍스트
        val midLat = (startPoint.latitude + endPoint.latitude) / 2
        val midLng = (startPoint.longitude + endPoint.longitude) / 2
        val midPoint = LatLng.from(midLat, midLng)

        val labelManager = map.labelManager
        var labelLayer = labelManager?.getLayer("walking_time_layer")
        if (labelLayer == null) {
            labelLayer = labelManager?.addLayer(LabelLayerOptions.from("walking_time_layer"))
        }

        // 텍스트 스타일
        val textStyle = LabelTextStyle.from(30, android.graphics.Color.BLACK, 4, android.graphics.Color.WHITE)

        // 1x1 픽셀짜리 투명한 비트맵 이미지를 배경으로 사용하기 위해 생성
        val transparentBitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        transparentBitmap.eraseColor(android.graphics.Color.TRANSPARENT)

        // 생성한 투명 비트맵을 아이콘으로 사용
        val labelStyle = LabelStyle.from(transparentBitmap).setTextStyles(textStyle)
        val labelStyles = labelManager?.addLabelStyles(LabelStyles.from(labelStyle))

        // 라벨 추가
        labelLayer?.addLabel(
            LabelOptions.from("walk_time_$id", midPoint)
                .setStyles(labelStyles)
                .setTexts(LabelTextBuilder().setTexts("🚶 약 ${walkTime}분"))
        )
    }

    LaunchedEffect(mapObj) {
        if (mapObj.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        val correctMapObj = if (mapObj.startsWith("0:0@")) mapObj else "0:0@$mapObj"
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.odsayService.getLoadLane(BuildConfig.ODSAY_API_KEY, correctMapObj)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) laneData = response.body()
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    // [전체 경로 및 도보 구간 렌더링]
    LaunchedEffect(laneData, kakaoMapInstance, myLocation) {
        val map = kakaoMapInstance ?: return@LaunchedEffect
        val data = laneData ?: return@LaunchedEffect
        val lanes = data.result?.lane ?: return@LaunchedEffect

        map.routeLineManager?.layer?.removeAll()
        map.labelManager?.getLayer("walking_time_layer")?.removeAll()

        var previousEndPoint: LatLng? = null

        lanes.forEachIndexed { index, laneDetail ->
            val points = laneDetail.section?.flatMap { it.graphPos ?: emptyList() }
                ?.map { LatLng.from(it.y, it.x) } ?: emptyList()

            if (points.isNotEmpty()) {
                val currentStartPoint = points.first()
                val currentEndPoint = points.last()

                // [도보 1] 내 위치 -> 첫 번째 대중교통 승차 지점
                if (index == 0 && myLocation != null) {
                    drawWalkingSection(map, myLocation!!, currentStartPoint, "start_to_transit")
                }

                // [도보 2] 환승 구간 (이전 대중교통 하차 지점 -> 현재 대중교통 승차 지점)
                if (previousEndPoint != null) {
                    drawWalkingSection(map, previousEndPoint!!, currentStartPoint, "transfer_$index")
                }

                previousEndPoint = currentEndPoint

                // [대중교통 노선 그리기]
                val lineColor = when (laneDetail.trafficClass) {
                    1 -> android.graphics.Color.parseColor("#4CAF50") // 지하철
                    2 -> android.graphics.Color.parseColor("#2B50A1") // 버스
                    else -> android.graphics.Color.parseColor("#F44336") // 그외인 경우
                }
                val styles = RouteLineStyles.from(RouteLineStyle.from(15f, lineColor))
                val styleSet = RouteLineStylesSet.from("transit_style_$index", styles)
                val segment = RouteLineSegment.from(points).setStyles(styleSet.getStyles(0))
                map.routeLineManager?.layer?.addRouteLine(RouteLineOptions.from(segment).setStylesSet(styleSet))
            }
        }

        // [도보 3] 마지막 하차 지점 -> 최종 목적지
        if (previousEndPoint != null && destLat != 0.0 && destLon != 0.0) {
            val destPoint = LatLng.from(destLat, destLon)
            drawWalkingSection(map, previousEndPoint!!, destPoint, "transit_to_dest")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        start(
                            object : MapLifeCycleCallback() {
                                override fun onMapDestroy() {}
                                override fun onMapError(error: Exception?) {}
                            },
                            object : KakaoMapReadyCallback() {
                                override fun onMapReady(map: KakaoMap) {
                                    kakaoMapInstance = map
                                    moveToMyLocation(map)
                                }
                            }
                        )
                    }
                }
            )

            FloatingActionButton(
                onClick = { kakaoMapInstance?.let { moveToMyLocation(it) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 40.dp, end = 20.dp),
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "내 위치")
            }
        }
    }
}
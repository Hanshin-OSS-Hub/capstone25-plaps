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
import com.example.plaps.data.PathItem
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextStyle
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.route.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransPathActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        val pathJson = intent.getStringExtra("PATH_JSON") ?: ""
        val destLat = intent.getDoubleExtra("DEST_LAT", 0.0)
        val destLon = intent.getDoubleExtra("DEST_LON", 0.0)

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                TransPathScreen(fusedLocationClient, pathJson, destLat, destLon)
            }
        }
    }
}

@Composable
fun TransPathScreen(
    fusedLocationClient: FusedLocationProviderClient,
    pathJson: String,
    destLat: Double,
    destLon: Double
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pathItem = remember { Gson().fromJson(pathJson, PathItem::class.java) }
    // mapObj 추출
    val mapObj = pathItem?.info?.mapObj ?: ""

    var laneData by remember { mutableStateOf<ODsayLaneResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var kakaoMapInstance by remember { mutableStateOf<KakaoMap?>(null) }
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
                        myLocation = position

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

    fun drawWalkingSection(map: KakaoMap, startPoint: LatLng, endPoint: LatLng, id: String) {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            startPoint.latitude, startPoint.longitude,
            endPoint.latitude, endPoint.longitude,
            results
        )
        val distance = results[0]
        if (distance < 20) return

        val walkTime = Math.ceil((distance / 67.0).toDouble()).toInt()
        val points = listOf(startPoint, endPoint)

        val style = RouteLineStyle.from(8f, android.graphics.Color.parseColor("#9E9E9E"))
        val styles = RouteLineStyles.from(style)
        val styleSet = RouteLineStylesSet.from("walk_style_$id", styles)
        val segment = RouteLineSegment.from(points).setStyles(styleSet.getStyles(0))
        map.routeLineManager?.layer?.addRouteLine(RouteLineOptions.from(segment).setStylesSet(styleSet))

        val midLat = (startPoint.latitude + endPoint.latitude) / 2
        val midLng = (startPoint.longitude + endPoint.longitude) / 2
        val midPoint = LatLng.from(midLat, midLng)

        val labelManager = map.labelManager
        var labelLayer = labelManager?.getLayer("walking_time_layer")
        if (labelLayer == null) {
            labelLayer = labelManager?.addLayer(LabelLayerOptions.from("walking_time_layer"))
        }

        val textStyle = LabelTextStyle.from(30, android.graphics.Color.BLACK, 4, android.graphics.Color.WHITE)
        val transparentBitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        transparentBitmap.eraseColor(android.graphics.Color.TRANSPARENT)
        val labelStyle = LabelStyle.from(transparentBitmap).setTextStyles(textStyle)
        val labelStyles = labelManager?.addLabelStyles(LabelStyles.from(labelStyle))

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

    // 기차/버스 여부를 판단해 지도 경로 표시
    LaunchedEffect(laneData, kakaoMapInstance, myLocation) {
        val map = kakaoMapInstance ?: return@LaunchedEffect
        val lanes = laneData?.result?.lane ?: emptyList()

        map.routeLineManager?.layer?.removeAll()
        map.labelManager?.getLayer("walking_time_layer")?.removeAll()

        var previousEndPoint: LatLng? = null
        var laneIndex = 0

        pathItem?.subPathList?.forEachIndexed { index, subPath ->
            if (subPath.trafficType in listOf(1, 2)) {
                // 지하철/버스
                val laneDetail = lanes.getOrNull(laneIndex)
                if (laneDetail != null) {
                    val points = laneDetail.section?.flatMap { it.graphPos ?: emptyList() }
                        ?.map { LatLng.from(it.y, it.x) } ?: emptyList()

                    if (points.isNotEmpty()) {
                        val currentStartPoint = points.first()
                        val currentEndPoint = points.last()

                        // 도보 연결
                        if (previousEndPoint != null) {
                            drawWalkingSection(map, previousEndPoint!!, currentStartPoint, "transfer_$index")
                        } else if (myLocation != null) {
                            drawWalkingSection(map, myLocation!!, currentStartPoint, "start_to_transit_$index")
                        }


                        val lineColor = when (laneDetail.trafficClass) {
                            1 -> android.graphics.Color.parseColor("#4CAF50") // 버스(초록)
                            2 -> android.graphics.Color.parseColor("#2B50A1") // 지하철(파랑)
                            else -> android.graphics.Color.parseColor("#F44336")
                        }
                        val styles = RouteLineStyles.from(RouteLineStyle.from(15f, lineColor))
                        val styleSet = RouteLineStylesSet.from("transit_style_$index", styles)
                        val segment = RouteLineSegment.from(points).setStyles(styleSet.getStyles(0))
                        map.routeLineManager?.layer?.addRouteLine(RouteLineOptions.from(segment).setStylesSet(styleSet))

                        previousEndPoint = currentEndPoint
                    }
                    laneIndex++
                }
            } else if (subPath.trafficType in listOf(4, 5, 6)) {
                // 기차/고속버스
                val sX = subPath.startX ?: 0.0
                val sY = subPath.startY ?: 0.0
                val eX = subPath.endX ?: 0.0
                val eY = subPath.endY ?: 0.0

                if (sX != 0.0 && sY != 0.0 && eX != 0.0 && eY != 0.0) {
                    val currentStartPoint = LatLng.from(sY, sX)
                    val currentEndPoint = LatLng.from(eY, eX)

                    // 역까지 걸어가는 길 연결
                    if (previousEndPoint != null) {
                        drawWalkingSection(map, previousEndPoint!!, currentStartPoint, "transfer_$index")
                    } else if (myLocation != null) {
                        drawWalkingSection(map, myLocation!!, currentStartPoint, "start_to_transit_$index")
                    }


                    // 기차(4)는 빨강, 시외/고속버스(5,6)는 보라색
                    val lineColor = when (subPath.trafficType) {
                        4 -> android.graphics.Color.parseColor("#E74C3C")
                        else -> android.graphics.Color.parseColor("#8E44AD")
                    }
                    val style = RouteLineStyle.from(15f, lineColor)
                    val styles = RouteLineStyles.from(style)
                    val styleSet = RouteLineStylesSet.from("train_style_$index", styles)

                    // 출발 역or터미널, 도착 역or터미널 선 연결
                    val segment = RouteLineSegment.from(listOf(currentStartPoint, currentEndPoint)).setStyles(styleSet.getStyles(0))
                    map.routeLineManager?.layer?.addRouteLine(RouteLineOptions.from(segment).setStylesSet(styleSet))

                    previousEndPoint = currentEndPoint
                }
            }
        }

        // 최종 목적지까지 가는 길 연결
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
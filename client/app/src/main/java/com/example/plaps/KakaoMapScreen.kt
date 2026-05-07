package com.example.plaps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.plaps.api.RetrofitClient
import com.example.plaps.api.service.local.PlaceAdapter
import com.example.plaps.databinding.ActivityLocationNpBinding
import com.example.plaps.domain.SearchResponse
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun KakaoMapScreen() {
    val context = LocalContext.current
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }

    // 내 현재 위치 정보를 저장할 State (NaviLoadActivity 전달용)
    var myCurrentLat by remember { mutableStateOf(0.0) }
    var myCurrentLon by remember { mutableStateOf(0.0) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // 위치 권한 요청 핸들러
    val locationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
            // 권한 허용됨
        }
    }

    AndroidView(
        factory = { ctx ->
            val binding = ActivityLocationNpBinding.inflate(LayoutInflater.from(ctx))

            // 1. 리사이클러뷰 및 어댑터 설정
            val placeAdapter = PlaceAdapter { place ->
                val intent = Intent(ctx, NaviLoadActivity::class.java).apply {
                    putExtra("DEST_NAME", place.placeName)
                    putExtra("DEST_LAT", place.y.toDouble())
                    putExtra("DEST_LON", place.x.toDouble())
                    putExtra("START_LAT", myCurrentLat)
                    putExtra("START_LON", myCurrentLon)
                }
                ctx.startActivity(intent)
            }

            binding.recyclerView.apply {
                layoutManager = LinearLayoutManager(ctx)
                adapter = placeAdapter
            }

            // 2. 카카오맵 초기화
            binding.mapView.start(object : MapLifeCycleCallback() {
                override fun onMapDestroy() {}
                override fun onMapError(error: Exception) {}
            }, object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map
                    // 초기 위치 설정
                    updateMyLocation(ctx, map, fusedLocationClient) { lat, lon ->
                        myCurrentLat = lat
                        myCurrentLon = lon
                    }
                }
            })

            // 3. 검색 버튼 클릭 이벤트
            binding.btnSearch.setOnClickListener {
                val keyword = binding.etSearchField.text.toString()
                executeSearch(ctx, keyword, kakaoMap, placeAdapter, binding)
            }

            // 4. 내 위치 버튼(FAB) 클릭 이벤트
            binding.fabMyLocation.setOnClickListener {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    updateMyLocation(ctx, kakaoMap, fusedLocationClient) { lat, lon ->
                        myCurrentLat = lat
                        myCurrentLon = lon
                    }
                } else {
                    locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                }
            }

            binding.root
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * 내 위치 갱신 및 마커 표시 함수
 */
private fun updateMyLocation(
    context: Context,
    map: KakaoMap?,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocationUpdated: (Double, Double) -> Unit
) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location ->
            location?.let {
                val lat = it.latitude
                val lon = it.longitude
                onLocationUpdated(lat, lon)

                val pos = LatLng.from(lat, lon)

                // 내 위치로 올 때는 지도의 패딩을 초기화하여 전체 화면 기준으로 중앙을 맞춤
                map?.setPadding(0, 0, 0, 0)
                map?.moveCamera(CameraUpdateFactory.newCenterPosition(pos, 17))

                val labelManager = map?.labelManager
                val layer = labelManager?.getLayer("my_location_layer")
                    ?: labelManager?.addLayer(LabelLayerOptions.from("my_location_layer"))

                layer?.removeAll()

                val styles = labelManager?.addLabelStyles(
                    LabelStyles.from(LabelStyle.from(android.R.drawable.btn_star_big_on).setAnchorPoint(0.5f, 0.5f))
                )
                // styles null 체크 후 마커 추가
                styles?.let { s ->
                    layer?.addLabel(LabelOptions.from(pos).setStyles(s))
                }
            }
        }
}

/**
 * 검색 및 마커 표시 로직 (목록 가림 방지 패딩 추가)
 */
private fun executeSearch(
    context: Context,
    keyword: String,
    kakaoMap: KakaoMap?,
    placeAdapter: PlaceAdapter,
    binding: ActivityLocationNpBinding
) {
    if (keyword.isBlank()) {
        Toast.makeText(context, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
        return
    }

    val apiKey = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}"
    RetrofitClient.kakaoLocalApiService.searchByKeyword(apiKey, keyword)
        .enqueue(object : Callback<SearchResponse> {
            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                if (response.isSuccessful) {
                    val places = response.body()?.documents
                    if (!places.isNullOrEmpty()) {
                        placeAdapter.submitList(places)
                        binding.recyclerView.visibility = View.VISIBLE

                        // [목록 가림 방지] 리사이클러뷰 높이(약 800px)만큼 지도의 하단 패딩 설정
                        // 이렇게 하면 지도의 실제 중심점이 리스트 위쪽으로 옮겨집니다.
                        kakaoMap?.setPadding(0, 0, 0, 800)

                        val labelManager = kakaoMap?.labelManager
                        val layer = labelManager?.getLayer("search_layer")
                            ?: labelManager?.addLayer(LabelLayerOptions.from("search_layer"))

                        layer?.removeAll()

                        val markerBitmap = vectorToBitmap(context, R.drawable.ic_marker)
                        if (markerBitmap != null) {
                            val style = LabelStyle.from(markerBitmap).setAnchorPoint(0.5f, 1.0f)
                            val styles = labelManager?.addLabelStyles(LabelStyles.from(style))

                            places.forEach { place ->
                                val pos = LatLng.from(place.y.toDouble(), place.x.toDouble())
                                styles?.let { s ->
                                    layer?.addLabel(LabelOptions.from(pos).setStyles(s))
                                }
                            }
                        }

                        // 첫 번째 장소가 화면 중앙(패딩 제외 영역의 중앙)에 오도록 이동
                        val firstPos = LatLng.from(places[0].y.toDouble(), places[0].x.toDouble())
                        kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(firstPos, 15))

                    } else {
                        binding.recyclerView.visibility = View.GONE
                        kakaoMap?.setPadding(0, 0, 0, 0)
                        Toast.makeText(context, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })

    // 키보드 숨기기
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(binding.etSearchField.windowToken, 0)
}

private fun vectorToBitmap(context: Context, drawableId: Int): Bitmap? {
    return try {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}
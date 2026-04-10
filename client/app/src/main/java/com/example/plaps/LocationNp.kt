package com.example.plaps

import android.Manifest
import android.content.Context
import android.content.Intent // ★ Intent 추가
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import com.example.plaps.domain.SearchResponse
import com.example.plaps.api.service.local.PlaceAdapter
import com.example.plaps.api.RetrofitClient
// ★ 파일 이름에 맞춰 바인딩 클래스 이름 확인하세요 (ActivityLocationNpBinding 권장)
import com.example.plaps.databinding.ActivityLocationNpBinding

class LocationNp : AppCompatActivity() {

    private lateinit var binding: ActivityLocationNpBinding
    private var kakaoMap: KakaoMap? = null
    private lateinit var placeAdapter: PlaceAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ★ [추가] 내 현재 위치를 저장해둘 변수
    private var myCurrentLat: Double = 0.0
    private var myCurrentLon: Double = 0.0

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> getCurrentLocation()
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> getCurrentLocation()
            else -> Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ★ 바인딩 이름 주의 (activity_location_np.xml -> ActivityLocationNpBinding)
        binding = ActivityLocationNpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRecyclerView()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() { Log.d("KakaoMap", "onMapDestroy") }
            override fun onMapError(error: Exception) { Log.e("KakaoMap", "onMapError: ", error) }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                Log.d("KakaoMap", "KakaoMap is ready!")
                this@LocationNp.kakaoMap = kakaoMap
                checkLocationPermission()
            }
        })

        binding.btnSearch.setOnClickListener {
            val keyword = binding.etSearchField.text.toString()
            searchByKeyword(keyword)
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearchField.windowToken, 0)
        }

        binding.etSearchField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchByKeyword(binding.etSearchField.text.toString())
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSearchField.windowToken, 0)
                true
            } else false
        }

        binding.fabMyLocation.setOnClickListener { checkLocationPermission() }
    }

// ★★★ [핵심 수정] 이제 NaviActivity가 아니라 NaviLoadActivity로 보냅니다! ★★★
    private fun initRecyclerView() {
        placeAdapter = PlaceAdapter { place ->
            Toast.makeText(this, "${place.placeName} 길안내를 준비합니다.", Toast.LENGTH_SHORT).show()

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearchField.windowToken, 0)

            // 1. NaviLoadActivity(로딩 화면)로 타겟 변경!
            val intent = Intent(this, NaviLoadActivity::class.java)

            // 2. 데이터 전달 (기존과 동일한 키값을 사용합니다)
            intent.putExtra("DEST_NAME", place.placeName)
            intent.putExtra("DEST_LAT", place.y.toDouble())
            intent.putExtra("DEST_LON", place.x.toDouble())

            // 출발지 정보도 같이 실어 보냅니다.
            intent.putExtra("START_LAT", myCurrentLat)
            intent.putExtra("START_LON", myCurrentLon)

            startActivity(intent)

            // 검색 화면은 닫아주는 게 깔끔하다면 주석 해제하세요.
            // finish()
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LocationNp)
            adapter = placeAdapter
        }
    }

    private fun searchByKeyword(keyword: String) {
        if (keyword.isBlank()) {
            Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
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

                            val labelManager = kakaoMap?.getLabelManager()
                            var layer = labelManager?.getLayer("search_layer")
                            if (layer == null) {
                                layer = labelManager?.addLayer(LabelLayerOptions.from("search_layer"))
                            }
                            layer?.removeAll()

                            val bitmap = vectorToBitmap(R.drawable.ic_marker)

                            if (bitmap != null) {
                                val style = LabelStyle.from(bitmap).setZoomLevel(0).setAnchorPoint(0.5f, 1.0f)
                                val styles = labelManager?.addLabelStyles(LabelStyles.from(style))

                                places.forEach { place ->
                                    val position = LatLng.from(place.y.toDouble(), place.x.toDouble())
                                    layer?.addLabel(LabelOptions.from(position).setStyles(styles))
                                }
                            }

                            val firstPlace = places[0]
                            kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(
                                LatLng.from(firstPlace.y.toDouble(), firstPlace.x.toDouble()), 15)
                            )
                            Toast.makeText(this@LocationNp, "총 ${places.size}개 장소 발견", Toast.LENGTH_SHORT).show()

                        } else {
                            binding.recyclerView.visibility = View.GONE
                            Toast.makeText(this@LocationNp, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LocationNp, "오류: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                    Toast.makeText(this@LocationNp, "API 호출 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun vectorToBitmap(drawableId: Int): Bitmap? {
        try {
            val drawable = ContextCompat.getDrawable(this, drawableId) ?: return null
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // ★★★ [중요 수정] 내 위치 찾으면 변수에 저장하는 코드 추가됨 ★★★
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {

                        // [추가된 부분] 내 위치를 전역 변수에 저장!
                        myCurrentLat = location.latitude
                        myCurrentLon = location.longitude

                        val position = LatLng.from(location.latitude, location.longitude)
                        kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(position, 17))

                        val labelManager = kakaoMap?.getLabelManager()
                        var layer = labelManager?.getLayer("my_location")
                        if(layer == null) {
                            layer = labelManager?.addLayer(LabelLayerOptions.from("my_location"))
                        }
                        layer?.removeAll()

                        val styles = labelManager?.addLabelStyles(
                            LabelStyles.from(
                                LabelStyle.from(android.R.drawable.btn_star_big_on)
                                    .setZoomLevel(0)
                            )
                        )
                        layer?.addLabel(LabelOptions.from(position).setStyles(styles))

                    } else {
                        Toast.makeText(this, "위치 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }
}
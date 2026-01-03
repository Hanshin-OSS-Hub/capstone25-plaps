package com.example.plaps

import android.Manifest
import android.content.Context
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
import com.example.plaps.databinding.ActivityLocationBinding
import com.example.plaps.api.RetrofitClient


class LocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationBinding
    private var kakaoMap: KakaoMap? = null
    private lateinit var placeAdapter: PlaceAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // 앱 첫 실행시 권한 여부, 허용되면 getCurrentLocation() 함수를 실행해 지도를 내 위치로 이동
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> getCurrentLocation()
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> getCurrentLocation()
            else -> Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }
    // 위치 서비스를 준비하는 초기 설정 단계
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRecyclerView()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    // 카카오맵 시작
        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() { Log.d("KakaoMap", "onMapDestroy") }
            override fun onMapError(error: Exception) { Log.e("KakaoMap", "onMapError: ", error) }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                Log.d("KakaoMap", "KakaoMap is ready!")
                this@LocationActivity.kakaoMap = kakaoMap
                checkLocationPermission()
            }
        })
    // 상단의 검색 버튼을 누르면 searchByKeyword()를 호출해 검색을 시작
        binding.btnSearch.setOnClickListener {
            val keyword = binding.etSearchField.text.toString()
            searchByKeyword(keyword)
            // 검색버튼을 누를경우 키보드가 자동으로 내려가서 불러온 검색목록을 가리지않도록
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearchField.windowToken, 0)


        }
    // 키보드 엔터를 누르면 searchByKeyword()를 호출해 검색을 시작
        binding.etSearchField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchByKeyword(binding.etSearchField.text.toString())
                // 엔터키 버튼을 누를경우 키보드가 자동으로 내려가서 불러온 검색목록을 가리지않도록
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSearchField.windowToken, 0)
                true
            } else false
        }
    //fabMyLocation 버튼을 누르면 권한을 다시 체크하고 내 위치로 이동
        binding.fabMyLocation.setOnClickListener { checkLocationPermission() }
    }

    private fun initRecyclerView() {
        placeAdapter = PlaceAdapter { place ->
            Toast.makeText(this, "${place.placeName} 선택!", Toast.LENGTH_SHORT).show()
            val position = LatLng.from(place.y.toDouble(), place.x.toDouble())
            kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(position, 17))
            binding.recyclerView.visibility = View.GONE

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearchField.windowToken, 0)

            // 선택된 장소 데이터를 Compose로 넘기고 종료
            val intent = android.content.Intent()

            // place.placeName은 "스타벅스 강남점" 같은 실제 장소 이름
            intent.putExtra("result_place_name", place.placeName)
            intent.putExtra("result_lat", place.y.toDouble())
            intent.putExtra("result_lng", place.x.toDouble())

            // 결과 설정 (OK 신호 + 데이터)
            setResult(RESULT_OK, intent)

            // 액티비티 종료 -> Compose 화면(편집창)으로 자동 복귀
            finish()
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LocationActivity)
            adapter = placeAdapter
        }
    }
    // 검색 기능
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

                            // getLayer() 괄호 안에 이름("search_layer")
                            var layer = labelManager?.getLayer("search_layer")
                            if (layer == null) {
                                layer = labelManager?.addLayer(LabelLayerOptions.from("search_layer"))
                            }
                            layer?.removeAll()

                            // 벡터 이미지를 비트맵으로 변환 (지도에 아이콘 나오게 하는 핵심)
                            val bitmap = vectorToBitmap(R.drawable.ic_marker)

                            if (bitmap != null) {
                                val style = LabelStyle.from(bitmap).setZoomLevel(0).setAnchorPoint(0.5f, 1.0f)
                                val styles = labelManager?.addLabelStyles(LabelStyles.from(style))

                                places.forEach { place ->
                                    val position = LatLng.from(place.y.toDouble(), place.x.toDouble())
                                    layer?.addLabel(LabelOptions.from(position).setStyles(styles))
                                }
                            } else {
                                Log.e("KakaoMap", "비트맵 변환 실패: ic_marker가 없거나 오류")
                            }

                            val firstPlace = places[0]
                            kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(
                                LatLng.from(firstPlace.y.toDouble(), firstPlace.x.toDouble()), 15)
                            )
                            Toast.makeText(this@LocationActivity, "총 ${places.size}개 장소 발견", Toast.LENGTH_SHORT).show()

                        } else {
                            binding.recyclerView.visibility = View.GONE
                            Toast.makeText(this@LocationActivity, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LocationActivity, "오류: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                    Toast.makeText(this@LocationActivity, "API 호출 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // 벡터 이미지를 비트맵으로 변환해주는 함수
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

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val position = LatLng.from(location.latitude, location.longitude)
                        kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(position, 17))

                        // 내 위치 마커 찍기
                        val labelManager = kakaoMap?.getLabelManager()
                        var layer = labelManager?.getLayer("my_location")
                        if(layer == null) {
                            layer = labelManager?.addLayer(LabelLayerOptions.from("my_location"))
                        }
                        layer?.removeAll()

                        // 내 위치는 기본 별모양 대신 안드로이드 기본 아이콘 사용 예시
                        val styles = labelManager?.addLabelStyles(
                            LabelStyles.from(
                                LabelStyle.from(android.R.drawable.btn_star_big_on) // 여기가 별 모양입니다
                                    .setZoomLevel(0) // 지도를 축소해도 별이 사라지지 않게 설정
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
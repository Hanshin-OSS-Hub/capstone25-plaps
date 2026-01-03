package com.example.plaps

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.kakaomobility.knsdk.KNLanguageType

import com.example.plaps.api.service.local.TransCoordRepository

class NaviLoadActivity : AppCompatActivity(){
    private val coordinateRepository = TransCoordRepository()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_naviload)

        checkPermission()
    }

    /**
     * GPS 위치 권한을 확인합니다.
     */
    fun checkPermission() {
        when {
            checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                // GPS 퍼미션 체크
                gpsPermissionCheck()
            }

            else -> {
                // 길찾기 SDK 인증
                knsdkAuth()
            }
        }
    }
    /**
     * GPS 위치 권한을 요청합니다.
     */
    fun gpsPermissionCheck() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1234)
    }
    /**
     * GPS 위치 권한 요청의 실패 여부를 확인합니다.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1234 -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // 다시 권한 요청하는 곳으로 돌아갑니다.
                    checkPermission()
                }
            }
        }
    }

    /**
     * 길찾기 SDK 인증을 진행합니다.
     */
    fun knsdkAuth() {
        GlobalApplication.knsdk.apply {
            initializeWithAppKey(
                aAppKey = BuildConfig.KAKAO_NATIVE_APP_KEY, // 카카오디벨로퍼스에서 부여 받은 앱 키
                aClientVersion = "1.0.0",                                               // 현재 앱의 클라이언트 버전
                aCsId = "testUser",                                                  // 사용자 id
                aLangType = KNLanguageType.KNLanguageType_KOREAN,   // 언어 타입
                aCompletion = {

                    runOnUiThread {
                        if (it != null) {
                            // 에러일 경우 띄워줌
                            Toast.makeText(applicationContext, "인증에 실패하였습니다: ${it.code}", Toast.LENGTH_LONG).show()
                        } else {
                            // 성공 메시지(필요 없는거 같다면 주석)
                             Toast.makeText(applicationContext, "인증 성공하였습니다", Toast.LENGTH_SHORT).show()

                            var intent = Intent(this@NaviLoadActivity, NaviActivity::class.java)

                            // 넘어온 목적지 정보를 받아옴
                            val destName = this@NaviLoadActivity.intent.getStringExtra("DEST_NAME")
                            val destLat = this@NaviLoadActivity.intent.getDoubleExtra("DEST_LAT", 0.0)
                            val destLon = this@NaviLoadActivity.intent.getDoubleExtra("DEST_LON", 0.0)

                            // NaviActivity로 다시 넘겨줍니다. (이게 없으면 NaviActivity가 목적지를 모름)
                            intent.putExtra("DEST_NAME", destName)
                            intent.putExtra("DEST_LAT", destLat)
                            intent.putExtra("DEST_LON", destLon)

                            this@NaviLoadActivity.startActivity(intent)

                            finish()
                        }
                    }
                })
        }
    }
}
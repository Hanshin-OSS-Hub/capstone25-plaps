package com.example.plaps

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk
import com.kakaomobility.knsdk.KNSDK
import dagger.hilt.android.HiltAndroidApp // 👈 Hilt Import

// 앱이 실행되는 순간 호출됨.
@HiltAndroidApp // 👈 Hilt 사용 선언
class GlobalApplication : Application() {
    // 카카오 내비용 opject

    companion object {
        lateinit var knsdk: KNSDK
    }

    override fun onCreate() {
        super.onCreate()
        // 카카오맵 SDK 초기화
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // 카카오내비 SDK 초기화.
        initialize()
    }

    // 길찾기 SDK 초기화.
    fun initialize() {
        knsdk = KNSDK.apply {
            //  파일 경로: data/data/com.kakaomobility.knsample/files/KNSample
            install(this@GlobalApplication, "$filesDir/KNSample")
        }
    }

}

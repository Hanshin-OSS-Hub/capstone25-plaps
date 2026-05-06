package com.example.plaps.api

import com.example.plaps.api.service.local.TransCoordService
import com.example.plaps.api.service.local.KakaoLocalApiService
// 새로 만든 ODsay
import com.example.plaps.api.service.local.ODsayService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object RetrofitClient{
    // 1. 각각의 기본 주소(Base URL) 분리
    private const val KAKAO_BASE_URL = "https://dapi.kakao.com/"
    private const val ODSAY_BASE_URL = "https://api.odsay.com/"

    // 2. 공통으로 사용할 통신 클라이언트 (통신 로그 확인용)
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    // 3. 카카오 전용 Retrofit 객체
    private val kakaoRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(KAKAO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 4. 오디세이 전용 Retrofit 객체 추가
    private val odsayRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ODSAY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // --- 아래는 실제 밖에서 호출해서 쓸 API 서비스들 ---

    // 기존 카카오 API 서비스
    val kakaoLocalApiService: KakaoLocalApiService by lazy {
        kakaoRetrofit.create(KakaoLocalApiService::class.java)
    }

    val kakaoLocalTransCoordApiService: TransCoordService by lazy {
        kakaoRetrofit.create(TransCoordService::class.java)
    }

    // 새로 추가한 오디세이 API 서비스
    val odsayService: ODsayService by lazy {
        odsayRetrofit.create(ODsayService::class.java)
    }
}
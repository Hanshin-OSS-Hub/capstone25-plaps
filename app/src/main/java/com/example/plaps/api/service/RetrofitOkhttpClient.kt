package com.example.plaps.api.service

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import com.example.plaps.api.service.local.KakaoLocalApiService

object RetrofitClient {
    private const val BASE_URL = "https://dapi.kakao.com"

    private val retrofit: Retrofit by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // 로그 레벨 설정 (BODY는 모든 내용을 보여줌)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()) // Gson을 사용해 JSON을 파싱
            .build()
    }

    val kakaoLocalApiService: KakaoLocalApiService by lazy {
        retrofit.create(KakaoLocalApiService::class.java)
    }
}

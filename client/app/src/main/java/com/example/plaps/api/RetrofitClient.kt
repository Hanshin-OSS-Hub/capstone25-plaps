package com.example.plaps.api

import com.example.plaps.api.service.local.TransCoordService
import com.example.plaps.api.service.local.KakaoLocalApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

// API 호출 할때 매번 조립해서 할 수는 없으니, 이거 한번 실행해두면 이후는 자동 조립.

object RetrofitClient{
    private const val BASE_URL = "https://dapi.kakao.com/"

    private val retrofit: Retrofit by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val kakaoLocalApiService: KakaoLocalApiService by lazy {
        retrofit.create(KakaoLocalApiService::class.java)
    }

    val kakaoLocalTransCoordApiService: TransCoordService = retrofit.create(TransCoordService::class.java)
}
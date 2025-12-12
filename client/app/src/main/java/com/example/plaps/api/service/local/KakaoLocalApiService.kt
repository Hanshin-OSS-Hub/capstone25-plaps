package com.example.plaps.api.service.local

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import com.example.plaps.domain.SearchResponse

interface KakaoLocalApiService {
    // 키워드로 장소를 검색하는 API
    @GET("/v2/local/search/keyword.json")
    fun searchByKeyword(
        @Header("Authorization") apiKey: String, // 인증을 위한 API 키
        @Query("query") keyword: String,       // 검색할 키워드
        @Query("size") size: Int = 15             // 한 번에 보여줄 검색 결과 개수
    ): Call<SearchResponse>
}

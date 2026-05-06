package com.example.plaps.api.service.local

import com.example.plaps.data.ODsayResponse
import com.example.plaps.data.ODsayLaneResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ODsayService {
    // 대중교통 길찾기 API 주소
   @GET("v1/api/searchPubTransPathT")
    suspend fun getTransitPath(
        @Query("apiKey") apiKey: String, // 발급받은 ODsay API 키
        @Query("SX") startX: String,     // 출발지 경도 (Longitude)
        @Query("SY") startY: String,     // 출발지 위도 (Latitude)
        @Query("EX") endX: String,       // 도착지 경도
        @Query("EY") endY: String        // 도착지 위도
    ): Response<ODsayResponse>

    // 대중교통 경로 안내
    @GET("v1/api/loadLane")
    suspend fun getLoadLane(
        @Query("apiKey") apiKey: String,       // 여기도 동일하게 키 필요
        @Query(value = "mapObject", encoded = true) mapObject: String // 앞서 받은 교환권 번호
    ): Response<ODsayLaneResponse>
}



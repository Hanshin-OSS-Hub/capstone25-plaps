package com.example.plaps.api.service.local

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

import com.example.plaps.domain.TransCoordResponse

interface TransCoordService {
    @GET("v2/local/geo/transcoord.json")
    suspend fun transWGS84toKTM(
        @Header("Authorization") apiKey: String,

        @Query("x") x: Double,
        @Query("y") y: Double,

        @Query("input_coord") inputCoord: String = "WGS84", // 이건 알아서 변경해서 넣어주면됨.
        @Query("output_coord") outputCoord: String = "KTM"
    ): TransCoordResponse
}
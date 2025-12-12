package com.example.plaps.api.service.local

import com.example.plaps.api.RetrofitClient
import com.example.plaps.domain.Document

class TransCoordRepository{
    val restApiKey = "본인의_REST_API_키" // 카카오 디벨로퍼스에서 복사한 키
    val authHeader = "KakaoAK $restApiKey" // 중요: "KakaoAK " 한 칸 띄우고 키 입력

    // 127.xxx(경도), 37.xxx(위도)를 KTM 좌표계로 변환 요청
    suspend fun transWGS84toKTM (x:Double, y:Double): Document?{
        return try {
                val response = RetrofitClient.kakaoLocalTransCoordApiService.transWGS84toKTM(
                    apiKey = authHeader,
                    x = x,
                    y = y,
                    outputCoord = "WTM" // 받고 싶은 좌표계
            )

            // 결과 꺼내쓰기
            if (response.documents.isNotEmpty()) response.documents[0] else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
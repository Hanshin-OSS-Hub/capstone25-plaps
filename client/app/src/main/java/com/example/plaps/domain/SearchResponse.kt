package com.example.plaps.domain

import com.google.gson.annotations.SerializedName

// 검색 결과를 담는 최상위 데이터 클래스
data class SearchResponse(
    @SerializedName("documents") val documents: List<Place>
)

// 개별 장소 정보를 담는 데이터 클래스
data class Place(
    @SerializedName("place_name") val placeName: String,     // 장소명
    @SerializedName("address_name") val addressName: String, // 전체 지번 주소
    @SerializedName("road_address_name") val roadAddressName: String, // 전체 도로명 주소
    @SerializedName("x") val x: String, // 경도(Longitude)
    @SerializedName("y") val y: String,  // 위도(Latitude)
    @SerializedName("id") val id: String
)

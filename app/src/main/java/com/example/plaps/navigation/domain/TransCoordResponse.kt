package com.example.plaps.navigation.domain
import com.google.gson.annotations.SerializedName

data class TransCoordResponse(
    @SerializedName("meta")
    val meta: Meta,

    @SerializedName("documents")
    val documents: List<Document>
)

// 2. "meta": { ... } 부분
data class Meta(
    @SerializedName("total_count")
    val totalCount: Int
)

// 3. "documents": [ ... ] 안의 내용물
data class Document(
    @SerializedName("x")
    val x: Double, // 숫자가 소수점이니까 Double

    @SerializedName("y")
    val y: Double
)


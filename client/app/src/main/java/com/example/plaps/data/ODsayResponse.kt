package com.example.plaps.data

import com.google.gson.annotations.SerializedName

// 전체 응답 데이터
data class ODsayResponse(
    @SerializedName("result") val result: ODsayResult?
)

// 결과 안에 담긴 전체 경로 목록
data class ODsayResult(
    @SerializedName("path") val pathList: List<PathItem>?
)

// 개별 경로
data class PathItem(
    @SerializedName("pathType") val pathType: Int, // 1: 지하철, 2: 버스, 3: 버스+지하철
    @SerializedName("info") val info: PathInfo,    // 요약 정보
    @SerializedName("subPath") val subPathList: List<SubPathItem> // 상세 이동 경로
)

// 요약 정보
data class PathInfo(
    @SerializedName("totalTime") val totalTime: Int, // 총 소요 시간 (분)
    @SerializedName("payment") val payment: Int,     // 총 요금 (원)
    @SerializedName("firstStartStation") val firstStartStation: String, // 첫 승차역
    @SerializedName("lastEndStation") val lastEndStation: String,       // 최종 하차역
    @SerializedName("mapObj") val mapObj: String? // 지도에 경로(선)를 그리기 위해 필요한 값
)

// 구간별 상세 정보

data class SubPathItem(
    @SerializedName("trafficType") val trafficType: Int, // 1: 지하철, 2: 시내버스, 3: 도보, 4: 기차(KTX 등), 5: 시외버스, 6: 고속버스
    @SerializedName("distance") val distance: Double,    // 이동 거리 (m)
    @SerializedName("sectionTime") val sectionTime: Int, // 구간 소요 시간 (분)
    @SerializedName("stationCount") val stationCount: Int?, // 정차 역 수
    @SerializedName("startName") val startName: String?,    // 승차역/정류장명
    @SerializedName("endName") val endName: String?,        // 하차역/정류장명
    @SerializedName("lane") val laneList: List<LaneItem>?,  // 노선 정보 (1호선, 2호선)
    @SerializedName("door") val door: String?,              // 내리는 문 방향
    @SerializedName("fastTrain") val fastTrain: String?,    // 빠른 환승/하차 번호

    // 승강장/정류장 방면 안내를 위한 종점 정보
    @SerializedName("way") val way: String? = null,

    // 경유하는 역/정류장 리스트 (이 중 1번을 꺼내어 ~역 방면 표기)
    @SerializedName("passStopList") val passStopList: PassStopList? = null,

    // 장거리 대중교통(기차/고속버스) 구간을 분할 호출할 때 사용할 출발/도착 좌표
    @SerializedName("startX") val startX: Double? = null,
    @SerializedName("startY") val startY: Double? = null,
    @SerializedName("endX") val endX: Double? = null,
    @SerializedName("endY") val endY: Double? = null
)

// 구간 내 경유하는 정거장들의 리스트 정보
data class PassStopList(
    @SerializedName("stations") val stations: List<Station>?
)

// 개별 경유 정거장 정보
data class Station(
    @SerializedName("stationName") val stationName: String?
)

// 노선 상세 정보
data class LaneItem(
    @SerializedName("name") val name: String?,         // 노선명 (수도권 1호선, KTX 등)
    @SerializedName("busNo") val busNo: String?,       // 버스 번호
    @SerializedName("subwayCode") val subwayCode: Int?
)


// 경로 선 그리기용
data class ODsayLaneResponse(
    @SerializedName("result") val result: LaneResult?,
    @SerializedName("error") val error: com.google.gson.JsonElement?    // 에러 찾는 용도
)

data class LaneResult(
    @SerializedName("lane") val lane: List<LaneDetail>? // 대중교통 노선들의 리스트
)

data class LaneDetail(
    @SerializedName("class") val trafficClass: Int, // 1: 버스, 2: 지하철
    @SerializedName("section") val section: List<LaneSection>? // 노선이 실제로 이동하는 구간
)

data class LaneSection(
    @SerializedName("graphPos") val graphPos: List<GraphPos>? // 선을 그리기 위한 점(좌표)들의 리스트
)

data class GraphPos(
    @SerializedName("x") val x: Double, // 경도
    @SerializedName("y") val y: Double  // 위도
)
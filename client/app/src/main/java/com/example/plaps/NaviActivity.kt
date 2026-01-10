package com.example.plaps

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.kakaomobility.knsdk.ui.view.KNNaviView
import android.graphics.Color
import android.util.Log
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.common.objects.KNError
import com.kakaomobility.knsdk.common.objects.KNPOI
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_CitsGuideDelegate

/* delegate import */
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_GuideStateDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_LocationGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_RouteGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_SafetyGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_VoiceGuideDelegate

import com.kakaomobility.knsdk.guidance.knguidance.KNGuideRouteChangeReason
import com.kakaomobility.knsdk.guidance.knguidance.citsguide.KNGuide_Cits
import com.kakaomobility.knsdk.guidance.knguidance.common.KNLocation
import com.kakaomobility.knsdk.guidance.knguidance.locationguide.KNGuide_Location
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.KNGuide_Route
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.objects.KNMultiRouteInfo
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.KNGuide_Safety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety
import com.kakaomobility.knsdk.guidance.knguidance.voiceguide.KNGuide_Voice
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute

/* 좌표변환 + 안내 메시지*/
import android.widget.Toast
import com.kakaomobility.knsdk.KNSDK

class NaviActivity : AppCompatActivity(), KNGuidance_GuideStateDelegate, KNGuidance_LocationGuideDelegate, KNGuidance_RouteGuideDelegate,
    KNGuidance_SafetyGuideDelegate, KNGuidance_VoiceGuideDelegate, KNGuidance_CitsGuideDelegate {

    lateinit var naviView: KNNaviView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navi)

        naviView = findViewById(R.id.navi_view)

        window?.apply {
            statusBarColor = Color.TRANSPARENT
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        requestRoute()
    }
    /**
     * 주행 경로를 요청합니다.
     */
    fun requestRoute() {
        // GPS가 없을 때 그냥 리턴하지 않고, 화면을 종료
        val gpsData = GlobalApplication.knsdk.sharedGpsManager()?.recentGpsData
        val pos = gpsData?.pos ?: run {
            Toast.makeText(this, "GPS 신호를 잡을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        // 이전 화면에서 넘겨준 목적지 정보(이름, 위도, 경도)를 받음

        val destName = intent.getStringExtra("DEST_NAME") ?: "목적지"
        val destLat = intent.getDoubleExtra("DEST_LAT", 0.0)
        val destLon = intent.getDoubleExtra("DEST_LON", 0.0)

        if (destLat == 0.0 || destLon == 0.0) {
            Toast.makeText(this, "등록된 위치 정보가 없어 안내할 수 없습니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 좌표 변환 (WGS84 -> KATEC)
        val katec = KNSDK.convertWGS84ToKATEC(destLon, destLat)

        // 변환된 좌표를 정수(Int)로 변환(에러 방지)
        val goalX = katec.x.toInt()
        val goalY = katec.y.toInt()

        Log.d("NAVI", "경로 요청: $destName ($destLat, $destLon) -> KATEC($goalX, $goalY)")


        // pos.x, pos.y도 .toInt()를 붙여서 안전하게 넣습니다.
        val startPoi = KNPOI("현위치", pos.x.toInt(), pos.y.toInt(), "현위치")
        val goalPoi = KNPOI(destName, goalX, goalY, destName)

        GlobalApplication.knsdk.makeTripWithStart(
            aStart = startPoi,
            aGoal = goalPoi,
            aVias = null
        ) { aError, aTrip ->
            if (aError == null && aTrip != null) {
                runOnUiThread { startGuide(aTrip) }
            } else {
                // 경로 탐색 실패 시 사용자에게 Toast 알림 표시
                runOnUiThread {
                    Toast.makeText(this, "경로 요청 실패: ${aError?.code}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    fun startGuide(trip: KNTrip?) {
        GlobalApplication.knsdk.sharedGuidance()?.apply {
            // guidance delegate 등록
            guideStateDelegate = this@NaviActivity
            locationGuideDelegate = this@NaviActivity
            routeGuideDelegate = this@NaviActivity
            safetyGuideDelegate = this@NaviActivity
            voiceGuideDelegate = this@NaviActivity
            citsGuideDelegate = this@NaviActivity
            // trip의 startWithTrip으로 안내를 진짜로 시작함.
            startWithTrip(
                aTrip = trip,
                aPriority = KNRoutePriority.KNRoutePriority_Recommand,
                aAvoidOptions = 0
            )

            naviView.initWithGuidance(
                this,
                trip,
                KNRoutePriority.KNRoutePriority_Recommand,
                0
            )
        }
    }

    // --- Delegate 메서드들 (유지) ---
    override fun guidanceCheckingRouteChange(aGuidance: KNGuidance) {
        naviView.guidanceCheckingRouteChange(aGuidance)
    }

    override fun guidanceDidUpdateIndoorRoute(aGuidance: KNGuidance, aRoute: KNRoute?) {
        naviView.guidanceDidUpdateIndoorRoute(aGuidance, aRoute)
    }

    override fun guidanceDidUpdateRoutes(aGuidance: KNGuidance, aRoutes: List<KNRoute>, aMultiRouteInfo: KNMultiRouteInfo?) {
        naviView.guidanceDidUpdateRoutes(aGuidance, aRoutes, aMultiRouteInfo)
    }

    override fun guidanceGuideEnded(aGuidance: KNGuidance) {
        naviView.guidanceGuideEnded(aGuidance)
    }

    override fun guidanceGuideStarted(aGuidance: KNGuidance) {
        naviView.guidanceGuideStarted(aGuidance)
    }

    override fun guidanceOutOfRoute(aGuidance: KNGuidance) {
        naviView.guidanceOutOfRoute(aGuidance)
    }

    override fun guidanceRouteChanged(aGuidance: KNGuidance, aFromRoute: KNRoute, aFromLocation: KNLocation, aToRoute: KNRoute, aToLocation: KNLocation, aChangeReason: KNGuideRouteChangeReason) {
        naviView.guidanceRouteChanged(aGuidance)
    }

    override fun guidanceRouteUnchanged(aGuidance: KNGuidance) {
        naviView.guidanceRouteUnchanged(aGuidance)
    }

    override fun guidanceRouteUnchangedWithError(aGuidnace: KNGuidance, aError: KNError) {
        naviView.guidanceRouteUnchangedWithError(aGuidnace, aError)
    }

    override fun guidanceDidUpdateLocation(aGuidance: KNGuidance, aLocationGuide: KNGuide_Location) {
        naviView.guidanceDidUpdateLocation(aGuidance, aLocationGuide)
    }

    override fun guidanceDidUpdateRouteGuide(aGuidance: KNGuidance, aRouteGuide: KNGuide_Route) {
        naviView.guidanceDidUpdateRouteGuide(aGuidance, aRouteGuide)
    }

    override fun guidanceDidUpdateAroundSafeties(aGuidance: KNGuidance, aSafeties: List<KNSafety>?) {
        naviView.guidanceDidUpdateAroundSafeties(aGuidance, aSafeties)
    }

    override fun guidanceDidUpdateSafetyGuide(aGuidance: KNGuidance, aSafetyGuide: KNGuide_Safety?) {
        naviView.guidanceDidUpdateSafetyGuide(aGuidance, aSafetyGuide)
    }

    override fun didFinishPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        naviView.didFinishPlayVoiceGuide(aGuidance, aVoiceGuide)
    }

    override fun shouldPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice, aNewData: MutableList<ByteArray>): Boolean {
        return naviView.shouldPlayVoiceGuide(aGuidance, aVoiceGuide, aNewData)
    }

    override fun willPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        naviView.willPlayVoiceGuide(aGuidance, aVoiceGuide)
    }

    override fun didUpdateCitsGuide(aGuidance: KNGuidance, aCitsGuide: KNGuide_Cits) {
        naviView.didUpdateCitsGuide(aGuidance, aCitsGuide)
    }
}
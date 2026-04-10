package com.example.plaps

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.common.objects.KNError
import com.kakaomobility.knsdk.common.objects.KNPOI
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_CitsGuideDelegate
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
import com.kakaomobility.knsdk.ui.view.KNNaviView

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
     * 수정됨: SDK GPS가 없으면 Intent로 받은 출발지를 사용하도록 변경
     */
    fun requestRoute() {
        // 1. 목적지 정보 받기
        val destName = intent.getStringExtra("DEST_NAME") ?: "목적지"
        val destLat = intent.getDoubleExtra("DEST_LAT", 0.0)
        val destLon = intent.getDoubleExtra("DEST_LON", 0.0)

        if (destLat == 0.0 || destLon == 0.0) {
            Toast.makeText(this, "목적지 정보가 없습니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 2. 출발지(내 위치) 결정하기
        // 일단 카카오 SDK에게 물어봅니다.
        val gpsData = GlobalApplication.knsdk.sharedGpsManager()?.recentGpsData

        var startX = 0
        var startY = 0

        if (gpsData?.pos != null) {
            // [A] SDK가 GPS를 잡고 있으면 그걸 씁니다.
            startX = gpsData.pos.x.toInt()
            startY = gpsData.pos.y.toInt()
            Log.d("NAVI", "SDK GPS 사용: $startX, $startY")
        } else {
            // [B] SDK가 모르면(null), 아까 검색화면에서 넘겨준 좌표를 씁니다!
            val intentStartLat = intent.getDoubleExtra("START_LAT", 0.0)
            val intentStartLon = intent.getDoubleExtra("START_LON", 0.0)

            if (intentStartLat != 0.0) {
                // 받은 좌표(WGS84)를 카카오 좌표(KATEC)로 변환
                val startKatec = KNSDK.convertWGS84ToKATEC(intentStartLon, intentStartLat)
                startX = startKatec.x.toInt()
                startY = startKatec.y.toInt()
                Log.d("NAVI", "Intent 출발지 사용: $startX, $startY")
            } else {
                // 둘 다 없으면 진짜 못 찾음 -> 종료
                Toast.makeText(this, "GPS 신호를 잡을 수 없습니다.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        }

        // 3. 목적지 좌표 변환 (WGS84 -> KATEC)
        val katec = KNSDK.convertWGS84ToKATEC(destLon, destLat)
        val goalX = katec.x.toInt()
        val goalY = katec.y.toInt()

        Log.d("NAVI", "경로 요청: $destName -> ($goalX, $goalY)")

        // 4. 길안내 요청
        val startPoi = KNPOI("현위치", startX, startY, "현위치")
        val goalPoi = KNPOI(destName, goalX, goalY, destName)

        GlobalApplication.knsdk.makeTripWithStart(
            aStart = startPoi,
            aGoal = goalPoi,
            aVias = null
        ) { aError, aTrip ->
            if (aError == null && aTrip != null) {
                runOnUiThread { startGuide(aTrip) }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "경로 요청 실패: ${aError?.code} / ${aError?.msg}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    fun startGuide(trip: KNTrip?) {
        GlobalApplication.knsdk.sharedGuidance()?.apply {
            guideStateDelegate = this@NaviActivity
            locationGuideDelegate = this@NaviActivity
            routeGuideDelegate = this@NaviActivity
            safetyGuideDelegate = this@NaviActivity
            voiceGuideDelegate = this@NaviActivity
            citsGuideDelegate = this@NaviActivity

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

    // --- Delegate 메서드들 (기존 유지) ---
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
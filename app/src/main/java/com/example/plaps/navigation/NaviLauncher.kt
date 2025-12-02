package com.example.plaps

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.plaps.api.service.TransCoordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object NaviLauncher {

    private val repository = TransCoordRepository()

    // ★ 핵심: context를 파라미터로 받습니다! (누가 부르든 상관없게 됨)
    fun startNavigation(context: Context, scope: CoroutineScope, x: Double, y: Double, name: String) {

        // 코루틴 스코프도 밖에서 빌려옴 (lifecycleScope 등)
        scope.launch {
            // 1. 좌표 변환 (IO 스레드에서 실행 추천)
            val result = withContext(Dispatchers.IO) {
                repository.transWGS84toKTM(x, y)
            }

            if (result != null) {
                // 2. Intent 생성 (여기서 context를 사용!)
                val intent = Intent(context, NaviActivity::class.java).apply {
                    putExtra("dest_x", result.x)
                    putExtra("dest_y", result.y)
                    putExtra("dest_name", name)
                }

                // ★ 여기가 변경 포인트!
                // MainActivity.startActivity가 아니라 context.startActivity로 변경
                context.startActivity(intent)

            } else {
                Toast.makeText(context, "좌표 변환 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
package com.example.plaps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback

@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    val context = LocalContext.current

    // 네이버 공식 가이드라인 컬러 (권장)
    val naverGreenColor = Color(0xFF03A94D)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PLAPS", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(60.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp) // 버튼 높이 규격
                .background(naverGreenColor, RoundedCornerShape(8.dp)) // 공식 배경색 및 곡률
                .clickable {
                    val callback = object : OAuthLoginCallback {
                        override fun onSuccess() { authViewModel.onLoginSuccess() }
                        override fun onFailure(httpStatus: Int, message: String) {}
                        override fun onError(errorCode: Int, message: String) {}
                    }
                    NaverIdLoginSDK.authenticate(context, callback)
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 1. 네이버 로고 'N' (임시 텍스트 플레이스홀더)
                // 가이드라인에 따라 N 로고 높이는 16px(dp) 이상이어야 함.
                Text(
                    text = "N",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )

                // 2. 가이드라인 규정 간격 8px(dp)
                Spacer(modifier = Modifier.width(8.dp))

                // 3. 레이블 '네이버 로그인'
                Text(
                    text = "네이버 로그인",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 게스트 로그인
        OutlinedButton(
            onClick = { authViewModel.onLoginSuccess(guest = true) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.Gray)
        ) {
            Text("게스트로 시작하기", color = Color.Gray, fontSize = 16.sp)
        }
    }
}
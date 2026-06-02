package com.example.plaps

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback

@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    val context = LocalContext.current

    // 컬러 정의
    val naverGreenColor = Color(0xFF03A94D)
    val googleLightGrayColor = Color(0xFFF2F2F2) // 구글 가이드라인 "보통(Light Gray)" 색상

    // 구글 로그인 설정 (build.gradle.kts에서 가져온 웹 클라이언트 ID 사용)
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    // 구글 로그인 결과 처리 런처
    val googleLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            // 뷰모델에 구글 로그인 성공(isGoogle = true) 신호 전달
            authViewModel.onLoginSuccess(isGoogle = true)
        } catch (e: ApiException) {
            Log.e("GoogleLogin", "구글 로그인 실패: ${e.statusCode}", e)
            Toast.makeText(context, "구글 로그인 실패", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PLAPS", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(60.dp))

        // 1. 네이버 로그인 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(naverGreenColor, RoundedCornerShape(8.dp))
                .clickable {
                    val callback = object : OAuthLoginCallback {
                        override fun onSuccess() { authViewModel.onLoginSuccess() }
                        override fun onFailure(httpStatus: Int, message: String) {
                            Toast.makeText(context, "네이버 로그인 실패", Toast.LENGTH_SHORT).show()
                        }
                        override fun onError(errorCode: Int, message: String) {}
                    }
                    NaverIdLoginSDK.authenticate(context, callback)
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "N", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "네이버 로그인", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 구글 로그인 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)  // 구글 전용 슬림 밸런스 유지
                .height(40.dp)       // 📐 가이드라인: 높이 40.dp 고정
                .background(
                    color = googleLightGrayColor,
                    shape = RoundedCornerShape(20.dp) // 📐 가이드라인: 완벽한 알약 모양
                )
                .clickable {
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleLoginLauncher.launch(googleSignInClient.signInIntent)
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp), // 📐 가이드라인: 좌우 패딩 12.dp
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 구글 공식 로고 이미지
                Image(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = "Google 로고",
                    modifier = Modifier.size(20.dp) // 📐 가이드라인: 로고 크기 20.dp 고정
                )

                var googleFontSize by remember { mutableStateOf(14.sp) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp), // 좌우 여백을 살짝 줄여 공간 확보
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Google 계정으로 로그인",
                        maxLines = 1,
                        softWrap = false,
                        onTextLayout = { textLayoutResult ->
                            if (textLayoutResult.didOverflowWidth) {
                                if (googleFontSize > 11.sp) {
                                    googleFontSize = (googleFontSize.value - 1f).sp
                                }
                            }
                        },
                        style = TextStyle(
                            fontSize = googleFontSize,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1F1F1F)
                        )
                    )
                }

                Spacer(modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 게스트 버튼
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
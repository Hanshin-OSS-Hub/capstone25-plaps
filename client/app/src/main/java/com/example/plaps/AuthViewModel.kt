package com.example.plaps

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfileResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail = _userEmail.asStateFlow()

    private val _nickname = MutableStateFlow(prefs.getString("nickname", "닉네임") ?: "닉네임")
    val nickname = _nickname.asStateFlow()

    private val _profileImageUri = MutableStateFlow(prefs.getString("profile_image", null))
    val profileImageUri = _profileImageUri.asStateFlow()

    init {
        checkAutoLogin()
    }

    private fun checkAutoLogin() {
        val naverToken = NaverIdLoginSDK.getAccessToken()
        if (naverToken != null) {
            _isLoggedIn.value = true
            fetchNaverProfile()
            return
        }

        val isGuest = prefs.getBoolean("is_guest", false)
        if (isGuest) {
            _isLoggedIn.value = true
            _userEmail.value = "게스트 계정"
            return
        }
    }

    fun onLoginSuccess(guest: Boolean = false) {
        if (guest) {
            _userEmail.value = "게스트 계정"
            prefs.edit().putBoolean("is_guest", true).apply()
        } else {
            prefs.edit().putBoolean("is_guest", false).apply()
            fetchNaverProfile()
        }
        _isLoggedIn.value = true
    }

    private fun fetchNaverProfile() {
        NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
            override fun onSuccess(result: NidProfileResponse) {
                _userEmail.value = result.profile?.email

                val naverName = result.profile?.nickname ?: result.profile?.name
                val savedNickname = prefs.getString("nickname", null)
                if (savedNickname == null && naverName != null) {
                    updateNickname(naverName)
                }
            }
            override fun onFailure(httpStatus: Int, message: String) {}
            override fun onError(errorCode: Int, message: String) {}
        })
    }

    fun updateNickname(newName: String) {
        _nickname.value = newName
        prefs.edit().putString("nickname", newName).apply()
    }

    // 사진을 앱 내부로 복사
    fun updateProfileImage(uriString: String?) {
        if (uriString == null) {
            _profileImageUri.value = null
            prefs.edit().remove("profile_image").apply()
            return
        }

        try {
            val uri = Uri.parse(uriString)
            // 1. 앱 내부 저장소에 profile_image.jpg 라는 빈 파일 생성
            val file = File(context.filesDir, "profile_image.jpg")

            // 2. 갤러리 사진의 데이터를 읽어서 내부 파일로 복사 (Stream 복사)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 3. 임시 URI 대신, 영구적인 내 앱 내부 파일 경로를 저장
            val localPath = file.absolutePath
            _profileImageUri.value = localPath
            prefs.edit().putString("profile_image", localPath).apply()

        } catch (e: Exception) {
            e.printStackTrace()
            // 혹시 실패할 경우를 대비한 폴백(Fallback)
            _profileImageUri.value = uriString
            prefs.edit().putString("profile_image", uriString).apply()
        }
    }

    fun logout() {
        NaverIdLoginSDK.logout()
        prefs.edit().clear().apply()

        val file = File(context.filesDir, "profile_image.jpg")
        if (file.exists()) {
            file.delete()
        }

        _isLoggedIn.value = false
        _userEmail.value = null
        _nickname.value = "닉네임"
        _profileImageUri.value = null
    }
}
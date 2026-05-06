package com.example.plaps

import android.content.Context
import androidx.lifecycle.ViewModel
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfileResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // 닉네임 상태 관리 (기본값: 닉네임)
    private val _nickname = MutableStateFlow(prefs.getString("nickname", "닉네임") ?: "닉네임")
    val nickname = _nickname.asStateFlow()

    // 프로필 사진 URI 상태 관리
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
            }
            override fun onFailure(httpStatus: Int, message: String) {}
            override fun onError(errorCode: Int, message: String) {}
        })
    }

    // 닉네임 변경 저장 함수
    fun updateNickname(newName: String) {
        _nickname.value = newName
        prefs.edit().putString("nickname", newName).apply()
    }

    // 프로필 사진 변경 저장 함수
    fun updateProfileImage(uriString: String?) {
        _profileImageUri.value = uriString
        prefs.edit().putString("profile_image", uriString).apply()
    }

    fun logout() {
        NaverIdLoginSDK.logout()
        prefs.edit().clear().apply()

        _isLoggedIn.value = false
        _userEmail.value = null
        _nickname.value = "닉네임"
        _profileImageUri.value = null
    }
}
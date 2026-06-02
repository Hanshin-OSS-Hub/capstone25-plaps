package com.example.plaps

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
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
        // 네이버 자동 로그인 확인
        val naverToken = NaverIdLoginSDK.getAccessToken()
        if (naverToken != null) {
            _isLoggedIn.value = true
            fetchNaverProfile()
            return
        }

        // 구글 자동 로그인 확인
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (googleAccount != null) {
            _isLoggedIn.value = true
            _userEmail.value = googleAccount.email

            // 저장된 커스텀 닉네임이 없으면 구글 프로필 이름 사용
            val savedNickname = prefs.getString("nickname", null)
            if (savedNickname == null && googleAccount.displayName != null) {
                updateNickname(googleAccount.displayName!!)
            }
            return
        }

        // 3. 게스트 자동 로그인 확인
        val isGuest = prefs.getBoolean("is_guest", false)
        if (isGuest) {
            _isLoggedIn.value = true
            _userEmail.value = "게스트 계정"
            return
        }
    }

    fun onLoginSuccess(guest: Boolean = false, isGoogle: Boolean = false) {
        if (guest) {
            _userEmail.value = "게스트 계정"
            prefs.edit().putBoolean("is_guest", true).apply()
        } else if (isGoogle) {
            // 구글 로그인 성공 시
            prefs.edit().putBoolean("is_guest", false).apply()
            val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (googleAccount != null) {
                _userEmail.value = googleAccount.email
                val savedNickname = prefs.getString("nickname", null)
                if (savedNickname == null && googleAccount.displayName != null) {
                    updateNickname(googleAccount.displayName!!)
                }
            }
        } else {
            // 네이버 로그인 성공 시
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

    fun updateProfileImage(uriString: String?) {
        if (uriString == null) {
            _profileImageUri.value = null
            prefs.edit().remove("profile_image").apply()
            return
        }

        try {
            val uri = Uri.parse(uriString)
            val file = File(context.filesDir, "profile_image.jpg")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val localPath = file.absolutePath
            _profileImageUri.value = localPath
            prefs.edit().putString("profile_image", localPath).apply()

        } catch (e: Exception) {
            e.printStackTrace()
            _profileImageUri.value = uriString
            prefs.edit().putString("profile_image", uriString).apply()
        }
    }

    fun logout() {
        NaverIdLoginSDK.logout()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut()

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
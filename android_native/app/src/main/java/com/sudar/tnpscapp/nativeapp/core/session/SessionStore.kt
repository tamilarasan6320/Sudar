package com.sudar.tnpscapp.nativeapp.core.session

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

data class UserSession(
    val id: Int,
    val name: String?,
    val mobile: String?,
)

class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ds = context.sessionDataStore

    private object Keys {
        val IsLoggedIn: Preferences.Key<Boolean> = booleanPreferencesKey("isLoggedIn")
        val Token: Preferences.Key<String> = stringPreferencesKey("token")
        val UserId: Preferences.Key<Int> = intPreferencesKey("userId")
        val UserName: Preferences.Key<String> = stringPreferencesKey("userName")
        val UserMobile: Preferences.Key<String> = stringPreferencesKey("userMobile")
        val SelectedExam: Preferences.Key<String> = stringPreferencesKey("selectedExam")
        val SelectedExamId: Preferences.Key<Int> = intPreferencesKey("selectedExamId")
        val IsPremium: Preferences.Key<Boolean> = booleanPreferencesKey("is_premium")
        val DeviceId: Preferences.Key<String> = stringPreferencesKey("deviceId")
        // Premium video cache
        val PremiumVideoVersion: Preferences.Key<String> = stringPreferencesKey("premium_video_version")
        val PremiumVideoLocalPath: Preferences.Key<String> = stringPreferencesKey("premium_video_local_path")
    }

    val isLoggedIn: Flow<Boolean> = ds.data.map { it[Keys.IsLoggedIn] ?: false }
    val token: Flow<String?> = ds.data.map { it[Keys.Token] }
    val userId: Flow<Int?> = ds.data.map { it[Keys.UserId] }
    val userName: Flow<String?> = ds.data.map { it[Keys.UserName] }
    val userMobile: Flow<String?> = ds.data.map { it[Keys.UserMobile] }
    val selectedExam: Flow<String?> = ds.data.map { it[Keys.SelectedExam] }
    val selectedExamId: Flow<Int?> = ds.data.map { it[Keys.SelectedExamId] }
    val isPremium: Flow<Boolean> = ds.data.map { it[Keys.IsPremium] ?: false }
    val deviceId: Flow<String?> = ds.data.map { it[Keys.DeviceId] }
    val premiumVideoVersion: Flow<String?> = ds.data.map { it[Keys.PremiumVideoVersion] }
    val premiumVideoLocalPath: Flow<String?> = ds.data.map { it[Keys.PremiumVideoLocalPath] }

    suspend fun ensureDeviceId(): String {
        val existing = deviceId.first()
        if (!existing.isNullOrBlank()) return existing

        val generated = UUID.randomUUID().toString().replace("-", "")
        ds.edit { it[Keys.DeviceId] = generated }
        return generated
    }

    suspend fun authHeaders(): Map<String, String> {
        val device = ensureDeviceId()
        val authToken = token.first()

        val headers = mutableMapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "X-Device-Id" to device,
        )
        if (!authToken.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $authToken"
        }
        return headers
    }

    suspend fun setLoggedIn(user: UserSession, token: String) {
        ds.edit {
            it[Keys.IsLoggedIn] = true
            it[Keys.Token] = token
            it[Keys.UserId] = user.id
            if (!user.name.isNullOrBlank()) it[Keys.UserName] = user.name
            if (!user.mobile.isNullOrBlank()) it[Keys.UserMobile] = user.mobile
        }
    }

    suspend fun setLoggedIn(value: Boolean) {
        ds.edit { it[Keys.IsLoggedIn] = value }
    }

    suspend fun updateUserName(name: String) {
        ds.edit { it[Keys.UserName] = name }
    }

    suspend fun setPremium(isPremium: Boolean) {
        ds.edit { it[Keys.IsPremium] = isPremium }
    }

    suspend fun setSelectedExam(name: String, id: Int) {
        ds.edit {
            it[Keys.SelectedExam] = name
            it[Keys.SelectedExamId] = id
        }
    }

    suspend fun setPremiumVideoCache(version: String, localPath: String) {
        ds.edit {
            it[Keys.PremiumVideoVersion] = version
            it[Keys.PremiumVideoLocalPath] = localPath
        }
    }

    suspend fun getPremiumVideoVersion(): String? = premiumVideoVersion.first()
    suspend fun getPremiumVideoLocalPath(): String? = premiumVideoLocalPath.first()

    suspend fun clearSession() {
        ds.edit {
            it[Keys.IsLoggedIn] = false
            it.remove(Keys.Token)
            it.remove(Keys.UserId)
            it.remove(Keys.UserName)
            it.remove(Keys.UserMobile)
            it.remove(Keys.SelectedExam)
            it.remove(Keys.SelectedExamId)
            it.remove(Keys.IsPremium)
            // NOTE: keep deviceId stable across logouts
        }
    }

    // Convenience suspend getters for ViewModels
    suspend fun getUserId(): Int? = userId.first()
    suspend fun getToken(): String? = token.first()
    suspend fun getUserName(): String? = userName.first()
    suspend fun getUserMobile(): String? = userMobile.first()
    suspend fun getSelectedExamName(): String? = selectedExam.first()
    suspend fun getSelectedExamId(): Int? = selectedExamId.first()
    suspend fun isPremium(): Boolean = isPremium.first()
    suspend fun isLoggedIn(): Boolean = isLoggedIn.first()
    suspend fun clear() = clearSession()
}


package com.sudar.tnpscapp.nativeapp.core.repo

import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.model.CreateUserRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.CreateUserResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.SendOtpRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.SendOtpResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.UserProfileDto
import com.sudar.tnpscapp.nativeapp.core.network.model.VerifyOtpRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.VerifyOtpResponse
import retrofit2.Response
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: SudarApi,
) {
    suspend fun sendOtp(mobile: String): Response<SendOtpResponse> {
        return api.sendOtp(SendOtpRequest(mobile = mobile))
    }

    suspend fun resendOtp(mobile: String): Response<SendOtpResponse> {
        return api.resendOtp(SendOtpRequest(mobile = mobile))
    }

    suspend fun verifyOtp(mobile: String, otp: String, deviceId: String): Response<VerifyOtpResponse> {
        return api.verifyOtp(
            VerifyOtpRequest(
                mobile = mobile,
                otp = otp,
                deviceId = deviceId,
            ),
        )
    }

    suspend fun createUser(req: CreateUserRequest): Response<CreateUserResponse> {
        return api.createUser(req)
    }

    suspend fun getUserProfile(): Result<UserProfileDto> {
        return try {
            val response = api.getUserProfile()
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.user?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("User data not found"))
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicSettings(key: String): Result<Any?> {
        return try {
            val response = api.getPublicSettings(key)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.get("success") == true) {
                    Result.success(body["value"])
                } else {
                    Result.failure(Exception("Failed to load settings"))
                }
            } else {
                Result.failure(Exception("Request failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


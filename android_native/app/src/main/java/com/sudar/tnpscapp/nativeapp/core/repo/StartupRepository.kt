package com.sudar.tnpscapp.nativeapp.core.repo

import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.model.BasicResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.SubscriptionStatusResponse
import retrofit2.Response
import javax.inject.Inject

class StartupRepository @Inject constructor(
    private val api: SudarApi,
) {
    suspend fun checkSession(userId: Int): Response<BasicResponse> {
        return api.checkSession(mapOf("user_id" to userId))
    }

    suspend fun subscriptionStatus(userId: Int): Response<SubscriptionStatusResponse> {
        return api.subscriptionStatus(userId)
    }
}


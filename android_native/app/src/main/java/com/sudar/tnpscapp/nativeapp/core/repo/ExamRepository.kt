package com.sudar.tnpscapp.nativeapp.core.repo

import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.model.ExamCategoriesResponse
import retrofit2.Response
import javax.inject.Inject

class ExamRepository @Inject constructor(
    private val api: SudarApi,
) {
    suspend fun getExamCategories(): Response<ExamCategoriesResponse> {
        return api.examCategories()
    }
}


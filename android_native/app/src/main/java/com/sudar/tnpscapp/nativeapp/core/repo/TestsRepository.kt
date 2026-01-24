package com.sudar.tnpscapp.nativeapp.core.repo

import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.model.SubmitTestResultRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.SubmitTestResultResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.TestCategoriesResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.TestDetailsResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.TestHistoryResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.TestQuestionsResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.TestSessionsResponse
import retrofit2.Response
import javax.inject.Inject

class TestsRepository @Inject constructor(
    private val api: SudarApi,
) {
    suspend fun getCategories(examId: Int?, userId: Int?): Response<TestCategoriesResponse> {
        return api.testCategories(examId = examId, userId = userId)
    }

    suspend fun getSessions(categoryId: Int): Response<TestSessionsResponse> {
        return api.testSessions(categoryId = categoryId)
    }

    suspend fun getQuestions(sessionId: Int): Response<TestQuestionsResponse> {
        return api.testQuestions(sessionId = sessionId, language = "en")
    }

    suspend fun submitResult(req: SubmitTestResultRequest): Response<SubmitTestResultResponse> {
        return api.submitTestResult(req)
    }

    suspend fun history(userId: Int, limit: Int = 50): Response<TestHistoryResponse> {
        return api.testHistory(userId = userId, limit = limit)
    }

    suspend fun testDetails(resultId: Int, userId: Int?): Response<TestDetailsResponse> {
        return api.testDetails(resultId = resultId, userId = userId)
    }

    /**
     * Get test history for progress analytics
     */
    suspend fun getTestHistory(userId: Int): Result<List<TestHistoryItem>> {
        return try {
            val response = api.testHistory(userId = userId, limit = 100)
            if (response.isSuccessful && response.body()?.success == true) {
                val items = response.body()?.history?.map { dto ->
                    TestHistoryItem(
                        id = dto.id,
                        sessionId = dto.sessionId,
                        testName = dto.testName,
                        categoryId = null, // Not in DTO
                        categoryName = dto.categoryName,
                        totalQuestions = dto.totalQuestions ?: 0,
                        correctAnswers = dto.correct ?: 0,
                        wrongAnswers = dto.wrong ?: 0,
                        skippedAnswers = dto.unanswered ?: 0,
                        score = dto.percentage?.toString(),
                        timeTaken = dto.timeTaken ?: 0,
                        completedAt = dto.submittedAt
                    )
                } ?: emptyList()
                Result.success(items)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load history"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get rankings
     */
    suspend fun getRankings(): Result<List<RankingItem>> {
        return try {
            val response = api.rankings()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.get("success") == true) {
                    val rankings = (body["rankings"] as? List<*>)?.mapNotNull { item ->
                        val map = item as? Map<*, *>
                        if (map != null) {
                            RankingItem(
                                userId = (map["user_id"] as? Number)?.toInt() ?: 0,
                                name = map["name"]?.toString() ?: "",
                                score = (map["score"] as? Number)?.toDouble() ?: 0.0,
                                rank = (map["rank"] as? Number)?.toInt() ?: 0
                            )
                        } else null
                    } ?: emptyList()
                    Result.success(rankings)
                } else {
                    Result.failure(Exception("Failed to load rankings"))
                }
            } else {
                Result.failure(Exception("Request failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get progress analytics
     */
    suspend fun getProgressAnalytics(userId: Int): Result<ProgressAnalytics> {
        return try {
            val response = api.progressAnalytics(userId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.get("success") == true) {
                    val overallStats = body["overall_stats"] as? Map<*, *>
                    val totalTests = (overallStats?.get("total_tests") as? Number)?.toInt() ?: 0
                    val avgScore = (overallStats?.get("avg_score") as? Number)?.toDouble() ?: 0.0
                    val rank = (body["rank"] as? Number)?.toInt() ?: 0
                    val streak = (body["streak"] as? Number)?.toInt() ?: 0

                    val trendList = (body["performance_trend"] as? List<*>)?.mapNotNull { item ->
                        val map = item as? Map<*, *>
                        val score = (map?.get("score") as? Number)?.toDouble()
                        if (score != null) PerformanceTrendDto(score) else null
                    } ?: emptyList()

                    val strengthsList = (body["strengths"] as? List<*>)?.mapNotNull { item ->
                        val map = item as? Map<*, *>
                        val name = map?.get("name")?.toString()
                        val score = (map?.get("avg_score") as? Number)?.toDouble()
                        if (name != null && score != null) StrengthWeaknessDto(name, score) else null
                    } ?: emptyList()

                    val weaknessesList = (body["weaknesses"] as? List<*>)?.mapNotNull { item ->
                        val map = item as? Map<*, *>
                        val name = map?.get("name")?.toString()
                        val score = (map?.get("avg_score") as? Number)?.toDouble()
                        if (name != null && score != null) StrengthWeaknessDto(name, score) else null
                    } ?: emptyList()

                    Result.success(ProgressAnalytics(
                        totalTests = totalTests,
                        avgScore = avgScore,
                        rank = rank,
                        streak = streak,
                        performanceTrend = trendList,
                        strengths = strengthsList,
                        weaknesses = weaknessesList
                    ))
                } else {
                    Result.failure(Exception(body?.get("message")?.toString() ?: "Failed to load analytics"))
                }
            } else {
                Result.failure(Exception("Request failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class TestHistoryItem(
    val id: Int,
    val sessionId: Int,
    val testName: String?,
    val sessionName: String = testName ?: "Test",
    val categoryId: Int?,
    val categoryName: String?,
    val totalQuestions: Int,
    val correctAnswers: Int?,
    val wrongAnswers: Int?,
    val skippedAnswers: Int?,
    val score: String?,
    val percentage: Double = score?.toDoubleOrNull() ?: 0.0,
    val timeTaken: Int,
    val completedAt: String?
)

data class RankingItem(
    val userId: Int,
    val name: String,
    val score: Double,
    val rank: Int
)

data class ProgressAnalytics(
    val totalTests: Int,
    val avgScore: Double,
    val rank: Int,
    val streak: Int,
    val performanceTrend: List<PerformanceTrendDto>,
    val strengths: List<StrengthWeaknessDto>,
    val weaknesses: List<StrengthWeaknessDto>
)

data class PerformanceTrendDto(
    val score: Double
)

data class StrengthWeaknessDto(
    val name: String,
    val avgScore: Double
)


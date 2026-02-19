package com.sudar.tnpscapp.nativeapp.core.network

import com.sudar.tnpscapp.nativeapp.core.network.model.BasicResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.CloseTicketRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.CreateTicketRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.CreateTicketResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.CreatePublicTicketRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.CreateUserRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.CreateUserResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.ExamCategoriesResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.MyTicketsResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.SendOtpRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.SendOtpResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.SubscriptionStatusResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.SubmitTestResultRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.SubmitTestResultResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.TicketDetailsResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.TestCategoriesResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.TestDetailsResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.TestHistoryResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.TestQuestionsResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.TestSessionsResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.UserProfileResponse
import com.sudar.tnpscapp.nativeapp.core.network.model.VerifyOtpRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.VerifyOtpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SudarApi {
    @POST("auth/send_otp.php")
    suspend fun sendOtp(@Body req: SendOtpRequest): Response<SendOtpResponse>

    @POST("auth/resend_otp.php")
    suspend fun resendOtp(@Body req: SendOtpRequest): Response<SendOtpResponse>

    @POST("auth/verify_otp.php")
    suspend fun verifyOtp(@Body req: VerifyOtpRequest): Response<VerifyOtpResponse>

    @POST("auth/check_session.php")
    suspend fun checkSession(@Body req: Map<String, Int>): Response<BasicResponse>

    @POST("users/create.php")
    suspend fun createUser(@Body req: CreateUserRequest): Response<CreateUserResponse>

    @GET("subscriptions/status.php")
    suspend fun subscriptionStatus(@Query("user_id") userId: Int): Response<SubscriptionStatusResponse>

    @GET("tests/get_exam_categories.php")
    suspend fun examCategories(): Response<ExamCategoriesResponse>

    @GET("tests/get_categories.php")
    suspend fun testCategories(
        @Query("exam_id") examId: Int?,
        @Query("user_id") userId: Int?,
    ): Response<TestCategoriesResponse>

    @GET("tests/get_sessions.php")
    suspend fun testSessions(
        @Query("category_id") categoryId: Int?,
    ): Response<TestSessionsResponse>

    @GET("tests/get_sessions.php")
    suspend fun allSessions(): Response<TestSessionsResponse>

    @GET("tests/get_questions.php")
    suspend fun testQuestions(
        @Query("session_id") sessionId: Int,
        @Query("language") language: String? = null,
    ): Response<TestQuestionsResponse>

    @POST("tests/submit_result.php")
    suspend fun submitTestResult(@Body req: SubmitTestResultRequest): Response<SubmitTestResultResponse>

    @GET("tests/get_history.php")
    suspend fun testHistory(
        @Query("user_id") userId: Int,
        @Query("limit") limit: Int = 50,
    ): Response<TestHistoryResponse>

    @GET("tests/get_test_details.php")
    suspend fun testDetails(
        @Query("result_id") resultId: Int,
        @Query("user_id") userId: Int? = null,
    ): Response<TestDetailsResponse>

    @GET("users/profile.php")
    suspend fun getUserProfile(): Response<UserProfileResponse>

    @POST("auth/truecaller_login.php")
    suspend fun truecallerLogin(@Body req: Map<String, String>): Response<VerifyOtpResponse>

    @GET("settings/get_public.php")
    suspend fun getPublicSettings(@Query("key") key: String): Response<Map<String, Any?>>

    @POST("feedback/submit.php")
    suspend fun submitFeedback(@Body req: @JvmSuppressWildcards Map<String, Any?>): Response<Map<String, Any?>>

    @GET("tests/get_progress_analytics.php")
    suspend fun progressAnalytics(@Query("user_id") userId: Int): Response<Map<String, Any?>>

    @GET("users/rankings.php")
    suspend fun rankings(): Response<Map<String, Any?>>

    @POST("subscriptions/create.php")
    suspend fun createSubscription(@Body req: @JvmSuppressWildcards Map<String, Any?>): Response<Map<String, Any?>>

    @POST("subscriptions/verify.php")
    suspend fun verifySubscription(@Body req: @JvmSuppressWildcards Map<String, Any?>): Response<Map<String, Any?>>

    @POST("subscriptions/trial_fee_create_order.php")
    suspend fun createTrialFeeOrder(@Body req: @JvmSuppressWildcards Map<String, Any?>): Response<Map<String, Any?>>

    @POST("subscriptions/trial_fee_verify.php")
    suspend fun verifyTrialFee(@Body req: @JvmSuppressWildcards Map<String, Any?>): Response<Map<String, Any?>>

    @POST("referrals/track_install.php")
    suspend fun trackReferralInstall(@Body req: @JvmSuppressWildcards Map<String, Any?>): Response<Map<String, Any?>>

    // ==================== SUPPORT TICKETS ====================

    @POST("tickets/create.php")
    suspend fun createTicket(@Body req: CreateTicketRequest): Response<CreateTicketResponse>

    @POST("tickets/create_public.php")
    suspend fun createTicketPublic(@Body req: CreatePublicTicketRequest): Response<CreateTicketResponse>

    @GET("tickets/my_list.php")
    suspend fun myTickets(
        @Query("user_id") userId: Int,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): Response<MyTicketsResponse>

    @GET("tickets/details.php")
    suspend fun ticketDetails(
        @Query("user_id") userId: Int,
        @Query("id") ticketId: Int,
    ): Response<TicketDetailsResponse>

    @POST("tickets/close.php")
    suspend fun closeTicket(@Body req: CloseTicketRequest): Response<BasicResponse>

    @GET("tests/get_performance.php")
    suspend fun getPerformance(
        @Query("user_id") userId: Int,
        @Query("period") period: String
    ): Response<Map<String, Any?>>

    @GET("tests/get_sessions.php")
    suspend fun getQuestionSessions(): Response<TestSessionsResponse>
}


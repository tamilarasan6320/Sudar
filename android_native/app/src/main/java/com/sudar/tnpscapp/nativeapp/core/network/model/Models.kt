package com.sudar.tnpscapp.nativeapp.core.network.model

import com.google.gson.annotations.SerializedName

data class BasicResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error_code") val errorCode: String? = null,
)

data class SendOtpRequest(
    @SerializedName("mobile") val mobile: String,
)

data class SendOtpResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("mobile") val mobile: String? = null,
    @SerializedName("expires_in") val expiresIn: Int? = null,
    @SerializedName("resend_in") val resendIn: Int? = null,
    @SerializedName("error_code") val errorCode: String? = null,
    @SerializedName("retry_after") val retryAfter: Int? = null,
)

data class VerifyOtpRequest(
    @SerializedName("mobile") val mobile: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("device_id") val deviceId: String,
)

data class VerifyOtpResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("is_new_user") val isNewUser: Boolean? = null,
    @SerializedName("mobile") val mobile: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("remaining_attempts") val remainingAttempts: Int? = null,
    @SerializedName("error_code") val errorCode: String? = null,
    @SerializedName("user") val user: UserDto? = null,
)

data class CreateUserRequest(
    @SerializedName("mobile") val mobile: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("age") val age: Int? = null,
    @SerializedName("district") val district: String? = null,
    @SerializedName("education") val education: String? = null,
    @SerializedName("language") val language: String = "en",
    @SerializedName("device_id") val deviceId: String,
)

data class CreateUserResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("user") val user: UserDto? = null,
)

data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("mobile") val mobile: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("age") val age: Int? = null,
    @SerializedName("district") val district: String? = null,
    @SerializedName("education") val education: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("profile_pic") val profilePic: String? = null,
    @SerializedName("referral_code") val referralCode: String? = null,
)

data class UserProfileResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("user") val user: UserProfileDto? = null,
)

data class UserProfileDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("mobile") val mobile: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("age") val age: Int? = null,
    @SerializedName("district") val district: String? = null,
    @SerializedName("education") val education: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("profile_pic") val profilePic: String? = null,
    @SerializedName("is_premium") val isPremium: Boolean? = null,
    @SerializedName("premium_expiry_date") val premiumExpiryDate: String? = null,
    @SerializedName("selected_exam_id") val selectedExamId: Int? = null,
    @SerializedName("selected_exam_name") val selectedExamName: String? = null,
    @SerializedName("referral_code") val referralCode: String? = null,
)

data class SubscriptionStatusResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("is_premium") val isPremium: Boolean? = null,
    @SerializedName("premium_source") val premiumSource: String? = null,
    @SerializedName("days_left") val daysLeft: Int? = null,
    @SerializedName("trial_days_left") val trialDaysLeft: Int? = null,
    @SerializedName("subscription_requires_payment") val subscriptionRequiresPayment: Boolean? = null,
    @SerializedName("message") val message: String? = null,
)

data class ExamCategoriesResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("categories") val categories: List<ExamCategoryDto>? = null,
    @SerializedName("message") val message: String? = null,
)

data class ExamCategoryDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
)

data class TestCategoriesResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("categories") val categories: List<TestCategoryDto>? = null,
    @SerializedName("message") val message: String? = null,
)

data class TestCategoryDto(
    @SerializedName("id") val id: Int,
    @SerializedName("exam_category_id") val examCategoryId: Int? = null,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("image_path") val imagePath: String? = null,
    @SerializedName("sessions_count") val sessionsCount: Int? = null,
    @SerializedName("completed_count") val completedCount: Int? = null,
)

data class TestSessionsResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("sessions") val sessions: List<QuestionSessionDto>? = null,
    @SerializedName("message") val message: String? = null,
)

data class QuestionSessionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("test_category_id") val testCategoryId: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("total_questions") val totalQuestions: Int? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("difficulty") val difficulty: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("actual_question_count") val actualQuestionCount: Int? = null,
)

data class TestQuestionsResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("questions") val questions: List<QuestionDto>? = null,
    @SerializedName("message") val message: String? = null,
)

data class QuestionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("session_id") val sessionId: Int? = null,
    @SerializedName("correct_answer") val correctAnswer: String? = null,
    @SerializedName("difficulty") val difficulty: String? = null,
    @SerializedName("marks") val marks: Float? = null,
    @SerializedName("negative_marks") val negativeMarks: Float? = null,
    @SerializedName("display_order") val displayOrder: Int? = null,
    @SerializedName("question_en") val questionEn: String? = null,
    @SerializedName("question_ta") val questionTa: String? = null,
    @SerializedName("option_a_en") val optionAEn: String? = null,
    @SerializedName("option_b_en") val optionBEn: String? = null,
    @SerializedName("option_c_en") val optionCEn: String? = null,
    @SerializedName("option_d_en") val optionDEn: String? = null,
    @SerializedName("option_a_ta") val optionATa: String? = null,
    @SerializedName("option_b_ta") val optionBTa: String? = null,
    @SerializedName("option_c_ta") val optionCTa: String? = null,
    @SerializedName("option_d_ta") val optionDTa: String? = null,
    @SerializedName("explanation_en") val explanationEn: String? = null,
    @SerializedName("explanation_ta") val explanationTa: String? = null,
    @SerializedName("has_english") val hasEnglish: Boolean? = null,
    @SerializedName("has_tamil") val hasTamil: Boolean? = null,
    @SerializedName("table_data") val tableData: Any? = null,
) {
    fun optionsEn(): List<String> = listOf(optionAEn, optionBEn, optionCEn, optionDEn).map { it ?: "" }
    fun optionsTa(): List<String> = listOf(optionATa, optionBTa, optionCTa, optionDTa).map { it ?: "" }
}

data class SubmitTestResultRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("time_taken") val timeTaken: Int,
    @SerializedName("answers") val answers: List<SubmitAnswerRequest>,
)

data class SubmitAnswerRequest(
    @SerializedName("question_id") val questionId: Int,
    @SerializedName("answer") val answer: String? = null,
    @SerializedName("time_spent") val timeSpent: Int? = null,
    @SerializedName("marked_for_review") val markedForReview: Int? = null,
)

data class SubmitTestResultResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("result") val result: SubmitResultDto? = null,
)

data class SubmitResultDto(
    @SerializedName("result_id") val resultId: Int,
    @SerializedName("total_questions") val totalQuestions: Int? = null,
    @SerializedName("attempted") val attempted: Int? = null,
    @SerializedName("correct") val correct: Int? = null,
    @SerializedName("wrong") val wrong: Int? = null,
    @SerializedName("unanswered") val unanswered: Int? = null,
    @SerializedName("score") val score: Float? = null,
    @SerializedName("percentage") val percentage: Float? = null,
    @SerializedName("rank") val rank: Int? = null,
    @SerializedName("time_taken") val timeTaken: Int? = null,
)

data class TestHistoryResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("history") val history: List<TestHistoryItemDto>? = null,
    @SerializedName("message") val message: String? = null,
)

data class TestHistoryItemDto(
    @SerializedName("id") val id: Int,
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("test_name") val testName: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("total_questions") val totalQuestions: Int? = null,
    @SerializedName("attempted") val attempted: Int? = null,
    @SerializedName("correct") val correct: Int? = null,
    @SerializedName("wrong") val wrong: Int? = null,
    @SerializedName("unanswered") val unanswered: Int? = null,
    @SerializedName("score") val score: Float? = null,
    @SerializedName("percentage") val percentage: Float? = null,
    @SerializedName("rank") val rank: Int? = null,
    @SerializedName("time_taken") val timeTaken: Int? = null,
    @SerializedName("submitted_at") val submittedAt: String? = null,
)

data class TestDetailsResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("result") val result: TestDetailsResultDto? = null,
    @SerializedName("answers") val answers: List<TestDetailsAnswerDto>? = null,
    @SerializedName("message") val message: String? = null,
)

data class TestDetailsResultDto(
    @SerializedName("id") val id: Int,
    @SerializedName("exam_name") val examName: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("session_name") val sessionName: String? = null,
    @SerializedName("total_questions") val totalQuestions: Int? = null,
    @SerializedName("attempted_questions") val attemptedQuestions: Int? = null,
    @SerializedName("correct_answers") val correctAnswers: Int? = null,
    @SerializedName("wrong_answers") val wrongAnswers: Int? = null,
    @SerializedName("percentage") val percentage: Float? = null,
    @SerializedName("time_taken") val timeTaken: Int? = null,
    @SerializedName("submitted_at") val submittedAt: String? = null,
    @SerializedName("status") val status: String? = null,
)

data class TestDetailsAnswerDto(
    @SerializedName("question_number") val questionNumber: Int? = null,
    @SerializedName("question_en") val questionEn: String? = null,
    @SerializedName("question_ta") val questionTa: String? = null,
    @SerializedName("option_a_en") val optionAEn: String? = null,
    @SerializedName("option_b_en") val optionBEn: String? = null,
    @SerializedName("option_c_en") val optionCEn: String? = null,
    @SerializedName("option_d_en") val optionDEn: String? = null,
    @SerializedName("option_a_ta") val optionATa: String? = null,
    @SerializedName("option_b_ta") val optionBTa: String? = null,
    @SerializedName("option_c_ta") val optionCTa: String? = null,
    @SerializedName("option_d_ta") val optionDTa: String? = null,
    @SerializedName("correct_answer") val correctAnswer: String? = null,
    @SerializedName("selected_answer") val selectedAnswer: String? = null,
    @SerializedName("is_correct") val isCorrect: Boolean? = null,
    @SerializedName("explanation_en") val explanationEn: String? = null,
    @SerializedName("explanation_ta") val explanationTa: String? = null,
    @SerializedName("time_spent") val timeSpent: Int? = null,
) {
    fun optionsEn(): List<String> = listOf(optionAEn, optionBEn, optionCEn, optionDEn).map { it ?: "" }
    fun optionsTa(): List<String> = listOf(optionATa, optionBTa, optionCTa, optionDTa).map { it ?: "" }
}

// ==================== SUPPORT TICKETS ====================

data class CreateTicketRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("subject") val subject: String,
    @SerializedName("message") val message: String,
    @SerializedName("category") val category: String = "general",
)

data class CreateTicketResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("id") val id: Int? = null,
)

data class CreatePublicTicketRequest(
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_mobile") val userMobile: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("message") val message: String,
    @SerializedName("category") val category: String = "general",
)

data class CloseTicketRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("id") val id: Int,
)

data class TicketDto(
    @SerializedName("id") val id: Int,
    @SerializedName("subject") val subject: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("admin_response") val adminResponse: String? = null,
    @SerializedName("admin_response_at") val adminResponseAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
)

data class TicketStatsDto(
    @SerializedName("total") val total: Int? = null,
    @SerializedName("pending") val pending: Int? = null,
    @SerializedName("reviewed") val reviewed: Int? = null,
    @SerializedName("resolved") val resolved: Int? = null,
    @SerializedName("closed") val closed: Int? = null,
)

data class MyTicketsResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("total_count") val totalCount: Int? = null,
    @SerializedName("tickets") val tickets: List<TicketDto>? = null,
    @SerializedName("stats") val stats: TicketStatsDto? = null,
    @SerializedName("message") val message: String? = null,
)

data class TicketDetailsResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("ticket") val ticket: TicketDto? = null,
    @SerializedName("message") val message: String? = null,
)


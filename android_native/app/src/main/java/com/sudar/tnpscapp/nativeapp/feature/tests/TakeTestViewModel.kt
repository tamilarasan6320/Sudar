package com.sudar.tnpscapp.nativeapp.feature.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.model.QuestionDto
import com.sudar.tnpscapp.nativeapp.core.network.model.SubmitAnswerRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.SubmitTestResultRequest
import com.sudar.tnpscapp.nativeapp.core.repo.TestsRepository
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TakeTestUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val questions: List<QuestionDto> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(), // questionId -> optionIndex (0..3)
    val markedForReview: Set<Int> = emptySet(), // questionId
    val totalTimeSec: Int = 0,
    val timeRemainingSec: Int = 0,
) {
    val currentQuestion: QuestionDto? get() = questions.getOrNull(currentIndex)
}

data class TestResultData(
    val testTitle: String,
    val categoryName: String,
    val questions: List<QuestionDto>,
    val selectedAnswers: Map<Int, Int>, // questionId -> optionIndex (0..3)
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val totalQuestions: Int,
    val timeTaken: Int,
    val percentage: Float,
)

sealed interface TakeTestEvent {
    data class Submitted(val resultId: Int, val resultData: TestResultData) : TakeTestEvent
}

@HiltViewModel
class TakeTestViewModel @Inject constructor(
    private val testsRepository: TestsRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    var uiState by mutableStateOf(TakeTestUiState())
        private set

    private var started = false
    private var timerJob: Job? = null
    private var testTitle: String = ""
    private var categoryName: String = ""

    private val eventsChannel = Channel<TakeTestEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    fun start(sessionId: Int, durationMinutes: Int, title: String = "", category: String = "") {
        if (started) return
        started = true
        testTitle = title
        categoryName = category

        val total = (durationMinutes.coerceAtLeast(1)) * 60
        uiState = uiState.copy(
            isLoading = true,
            error = null,
            totalTimeSec = total,
            timeRemainingSec = total,
        )

        viewModelScope.launch {
            loadQuestions(sessionId)
        }
    }

    private suspend fun loadQuestions(sessionId: Int) {
        try {
            val res = testsRepository.getQuestions(sessionId)
            val body = res.body()
            if (res.isSuccessful && body?.success == true) {
                val qs = body.questions.orEmpty().filter { (it.hasEnglish == true) || (it.hasTamil == true) }
                if (qs.isEmpty()) {
                    uiState = uiState.copy(isLoading = false, error = "This test has no questions yet.")
                    return
                }
                uiState = uiState.copy(isLoading = false, questions = qs, currentIndex = 0)
                startTimer(sessionId)
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    error = body?.message ?: "Failed to load questions (HTTP ${res.code()})",
                )
            }
        } catch (e: Exception) {
            uiState = uiState.copy(isLoading = false, error = e.message ?: "Network error")
        }
    }

    private fun startTimer(sessionId: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (uiState.timeRemainingSec > 0 && !uiState.isSubmitting) {
                delay(1_000)
                uiState = uiState.copy(timeRemainingSec = (uiState.timeRemainingSec - 1).coerceAtLeast(0))
            }
            if (uiState.timeRemainingSec == 0 && !uiState.isSubmitting) {
                submit(sessionId)
            }
        }
    }

    fun selectOption(optionIndex: Int) {
        val q = uiState.currentQuestion ?: return
        uiState = uiState.copy(
            selectedAnswers = uiState.selectedAnswers + (q.id to optionIndex),
        )
    }

    fun toggleMarkForReview() {
        val q = uiState.currentQuestion ?: return
        val set = uiState.markedForReview.toMutableSet()
        if (set.contains(q.id)) set.remove(q.id) else set.add(q.id)
        uiState = uiState.copy(markedForReview = set)
    }

    fun next() {
        if (uiState.currentIndex < uiState.questions.size - 1) {
            uiState = uiState.copy(currentIndex = uiState.currentIndex + 1)
        }
    }

    fun previous() {
        if (uiState.currentIndex > 0) {
            uiState = uiState.copy(currentIndex = uiState.currentIndex - 1)
        }
    }
    
    fun goToQuestion(index: Int) {
        if (index in 0 until uiState.questions.size) {
            uiState = uiState.copy(currentIndex = index)
        }
    }

    fun submit(sessionId: Int) {
        if (uiState.isSubmitting) return
        timerJob?.cancel()
        uiState = uiState.copy(isSubmitting = true, error = null)

        viewModelScope.launch {
            try {
                val userId = sessionStore.userId.first()
                if (userId == null) {
                    uiState = uiState.copy(isSubmitting = false, error = "Please login again.")
                    return@launch
                }

                val timeTaken = (uiState.totalTimeSec - uiState.timeRemainingSec).coerceAtLeast(0)
                val startedAt = LocalDateTime.now()
                    .minusSeconds(timeTaken.toLong())
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                val answers = uiState.questions.map { q ->
                    val selectedIndex = uiState.selectedAnswers[q.id]
                    val answerChar = selectedIndex?.let { ('A'.code + it).toChar().toString() }
                    val marked = if (uiState.markedForReview.contains(q.id)) 1 else 0
                    SubmitAnswerRequest(
                        questionId = q.id,
                        answer = answerChar,
                        timeSpent = 0,
                        markedForReview = marked,
                    )
                }

                val res = testsRepository.submitResult(
                    SubmitTestResultRequest(
                        userId = userId,
                        sessionId = sessionId,
                        startedAt = startedAt,
                        timeTaken = timeTaken,
                        answers = answers,
                    ),
                )

                val body = res.body()
                if ((res.isSuccessful || res.code() == 201) && body?.success == true) {
                    val resultId = body.result?.resultId ?: 0
                    
                    // Calculate results from in-memory data (like Flutter does)
                    var correctCount = 0
                    var wrongCount = 0
                    for (q in uiState.questions) {
                        val selectedIndex = uiState.selectedAnswers[q.id]
                        if (selectedIndex != null) {
                            val selectedChar = ('A'.code + selectedIndex).toChar().toString()
                            if (selectedChar.equals(q.correctAnswer, ignoreCase = true)) {
                                correctCount++
                            } else {
                                wrongCount++
                            }
                        }
                    }
                    val totalQ = uiState.questions.size
                    val pct = if (totalQ > 0) (correctCount * 100f / totalQ) else 0f
                    
                    val resultData = TestResultData(
                        testTitle = testTitle,
                        categoryName = categoryName,
                        questions = uiState.questions,
                        selectedAnswers = uiState.selectedAnswers,
                        correctAnswers = correctCount,
                        wrongAnswers = wrongCount,
                        totalQuestions = totalQ,
                        timeTaken = timeTaken,
                        percentage = pct,
                    )
                    
                    eventsChannel.send(TakeTestEvent.Submitted(resultId, resultData))
                } else {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        error = body?.message ?: "Submit failed (HTTP ${res.code()})",
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(isSubmitting = false, error = e.message ?: "Network error")
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}


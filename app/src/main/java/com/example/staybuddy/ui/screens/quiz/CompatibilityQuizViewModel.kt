package com.example.staybuddy.ui.screens.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.repository.AuthRepository
import com.example.staybuddy.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizQuestion(
    val id: String,
    val text: String,
    val options: List<String>,
    val icon: String // Emoji or icon name
)

data class CompatibilityQuizUiState(
    val currentQuestionIndex: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null,
    val questions: List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "cleanliness",
            text = "How do you feel about cleanliness?",
            options = listOf("Neat Freak", "Moderately Clean", "Lived-in", "Messy but Organized"),
            icon = "✨"
        ),
        QuizQuestion(
            id = "sleep_schedule",
            text = "What is your sleep schedule?",
            options = listOf("Early Bird", "Night Owl", "Flexible", "Shift Worker"),
            icon = "🌙"
        ),
        QuizQuestion(
            id = "social_habit",
            text = "How often do you like having guests over?",
            options = listOf("Never", "Occasionally", "Frequently", "Always a Party"),
            icon = "🤝"
        ),
        QuizQuestion(
            id = "noise_level",
            text = "What is your preferred noise level at home?",
            options = listOf("Pin-drop Silence", "Library Quiet", "Background Music/TV", "Lively & Loud"),
            icon = "🎧"
        ),
        QuizQuestion(
            id = "dietary_preference",
            text = "Any dietary preferences/restrictions?",
            options = listOf("Vegetarian", "Non-Vegetarian", "Vegan", "No Preference"),
            icon = "🥗"
        ),
        QuizQuestion(
            id = "smoking_habit",
            text = "What is your smoking habit?",
            options = listOf("Non-Smoker", "Occasional Smoker", "Regular Smoker", "Outside Only"),
            icon = "🚭"
        ),
        QuizQuestion(
            id = "pet_friendly",
            text = "Are you comfortable with pets?",
            options = listOf("Love Pets", "Comfortable with Small Pets", "No Pets Please", "Allergic"),
            icon = "🐾"
        ),
        QuizQuestion(
            id = "alcohol_habit",
            text = "What is your drinking habit?",
            options = listOf("Non-Drinker", "Occasional/Social", "Regular", "Outside Only"),
            icon = "🍷"
        ),
        QuizQuestion(
            id = "work_schedule",
            text = "What is your work/study schedule?",
            options = listOf("9-5 Office", "Work from Home", "Student Schedule", "Irregular Hours"),
            icon = "💼"
        ),
        QuizQuestion(
            id = "hobbies",
            text = "What do you do in your free time?",
            options = listOf("Gaming/Tech", "Fitness/Sports", "Reading/Art", "Outdoors/Travel"),
            icon = "🎨"
        )
    )
)

@HiltViewModel
class CompatibilityQuizViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompatibilityQuizUiState())
    val uiState: StateFlow<CompatibilityQuizUiState> = _uiState.asStateFlow()

    fun onAnswerSelected(answer: String) {
        val currentState = _uiState.value
        val questionId = currentState.questions[currentState.currentQuestionIndex].id
        val newAnswers = currentState.answers.toMutableMap().apply {
            put(questionId, answer)
        }

        if (currentState.currentQuestionIndex < currentState.questions.size - 1) {
            _uiState.update { 
                it.copy(
                    answers = newAnswers,
                    currentQuestionIndex = it.currentQuestionIndex + 1
                )
            }
        } else {
            _uiState.update { it.copy(answers = newAnswers) }
            submitQuiz(newAnswers)
        }
    }

    fun onBackClicked() {
        if (_uiState.value.currentQuestionIndex > 0) {
            _uiState.update { it.copy(currentQuestionIndex = it.currentQuestionIndex - 1) }
        }
    }

    private fun submitQuiz(answers: Map<String, String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId != null) {
                    userRepository.updateQuizResults(userId, answers)
                    _uiState.update { it.copy(isSubmitting = false, isCompleted = true) }
                } else {
                    _uiState.update { it.copy(isSubmitting = false, error = "User not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, error = e.message) }
            }
        }
    }
}

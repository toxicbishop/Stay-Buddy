package com.example.staybuddy.ui.screens.quiz

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.staybuddy.ui.theme.StayBuddyTheme

@Composable
fun CompatibilityQuizScreen(
    onQuizCompleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: CompatibilityQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onQuizCompleted()
        }
    }

    CompatibilityQuizScreenContent(
        uiState = uiState,
        onBack = onBack,
        onAnswerSelected = viewModel::onAnswerSelected,
        onBackClicked = viewModel::onBackClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityQuizScreenContent(
    uiState: CompatibilityQuizUiState,
    onBack: () -> Unit,
    onAnswerSelected: (String) -> Unit,
    onBackClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Lifestyle Quiz",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isSubmitting) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(strokeWidth = 6.dp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Saving your lifestyle preferences...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    com.example.staybuddy.ui.components.ErrorBanner(
                        message = uiState.error!!,
                        onRetry = null
                    )
                }
            } else if (uiState.questions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    com.example.staybuddy.ui.components.EmptyState(
                        icon = Icons.AutoMirrored.Filled.FactCheck,
                        title = "No Questions Available",
                        message = "Lifestyle quiz questions are currently unavailable. Please try again later."
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Progress Indicator
                    val progress = if (uiState.questions.isNotEmpty()) {
                        (uiState.currentQuestionIndex + 1).toFloat() / uiState.questions.size
                    } else {
                        0f
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${uiState.currentQuestionIndex + 1} of ${uiState.questions.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    if (uiState.questions.isNotEmpty() && uiState.currentQuestionIndex < uiState.questions.size) {
                        // Question Content
                        val question = uiState.questions[uiState.currentQuestionIndex]
                        
                        AnimatedContent(
                            targetState = question,
                            transitionSpec = {
                                if (targetState.id != initialState.id) {
                                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> -width } + fadeOut()
                                    )
                                } else {
                                    fadeIn() togetherWith fadeOut()
                                }
                            },
                            label = "QuestionAnimation"
                        ) { targetQuestion ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            MaterialTheme.shapes.large
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(targetQuestion.icon, style = MaterialTheme.typography.displayMedium)
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    text = targetQuestion.text,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black
                                )
                                
                                Spacer(modifier = Modifier.height(40.dp))
                                
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    targetQuestion.options.forEach { option ->
                                        val isSelected = uiState.answers[targetQuestion.id] == option
                                        
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onAnswerSelected(option) },
                                            shape = MaterialTheme.shapes.large,
                                            color = if (isSelected) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(24.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = option,
                                                    modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (uiState.currentQuestionIndex > 0) {
                        TextButton(
                            onClick = { onBackClicked() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Previous Question", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompatibilityQuizScreenPreview() {
    StayBuddyTheme {
        CompatibilityQuizScreenContent(
            uiState = CompatibilityQuizUiState(
                currentQuestionIndex = 0,
                questions = listOf(
                    QuizQuestion(
                        id = "cleanliness",
                        text = "How do you feel about cleanliness?",
                        options = listOf("Neat Freak", "Moderately Clean", "Lived-in", "Messy but Organized"),
                        icon = "✨"
                    )
                ),
                answers = emptyMap()
            ),
            onBack = {},
            onAnswerSelected = {},
            onBackClicked = {}
        )
    }
}

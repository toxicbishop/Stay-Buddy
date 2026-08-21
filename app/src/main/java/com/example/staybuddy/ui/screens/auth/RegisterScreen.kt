package com.example.staybuddy.ui.screens.auth

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.staybuddy.ui.components.mouseWheelScroll
import com.example.staybuddy.ui.theme.StayBuddyTheme
import com.example.staybuddy.ui.theme.SuccessGreen
import com.example.staybuddy.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegistrationComplete: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToFinishRegistration: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isRegisterSuccess) {
        if (uiState.isRegisterSuccess) {
            onRegistrationComplete(uiState.role)
        }
    }

    RegisterContent(
        uiState = uiState,
        onRoleChange = viewModel::onRoleChange,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPhoneChange = viewModel::onPhoneChange,
        onPasswordChange = viewModel::onPasswordChange,
        onGenderChange = viewModel::onGenderChange,
        onCityChange = viewModel::onCityChange,
        onCollegeChange = viewModel::onCollegeChange,
        onRegister = viewModel::register,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterContent(
    uiState: RegisterUiState,
    onRoleChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onCollegeChange: (String) -> Unit,
    onRegister: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    // Live Password Strength calculations
    val password = uiState.password
    val hasLength = password.length >= 6
    val hasNumber = password.any { it.isDigit() }
    val hasUpper = password.any { it.isUpperCase() }
    val strengthScore = listOf(hasLength, hasNumber, hasUpper).count { it }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding()
                .mouseWheelScroll(scrollState)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clean Aligned Header (Back Icon + Title + Subtitle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onNavigateBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Join StayBuddy to find rooms & roommates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compact Role Choice Cards
            Text(
                text = "I'm joining as",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val roles = listOf(
                    Triple("student", "Student / Seeker", "Find PGs & Mates"),
                    Triple("owner", "Property Host", "List PGs")
                )

                roles.forEach { (roleValue, label, sub) ->
                    val isSelected = uiState.role == roleValue
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRoleChange(roleValue)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (roleValue == "student") Icons.Default.School else Icons.Default.HomeWork,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Form Container Card (Polished Dark & Light Mode Container)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Your Profile Info",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Helper for Compact Designer Form Input Fields
                    @Composable
                    fun CompactFormField(
                        label: String,
                        value: String,
                        onValueChange: (String) -> Unit,
                        placeholder: String,
                        leadingIcon: ImageVector,
                        keyboardType: KeyboardType = KeyboardType.Text,
                        trailingIcon: @Composable (() -> Unit)? = null,
                        visualTransformation: VisualTransformation = VisualTransformation.None
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                            OutlinedTextField(
                                value = value,
                                onValueChange = onValueChange,
                                placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = leadingIcon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = trailingIcon,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                                visualTransformation = visualTransformation,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    CompactFormField(
                        label = "Full Name",
                        value = uiState.name,
                        onValueChange = onNameChange,
                        placeholder = "e.g. Rahul Sharma",
                        leadingIcon = Icons.Default.Person
                    )

                    CompactFormField(
                        label = "Email Address",
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        placeholder = "e.g. rahul@example.com",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )

                    CompactFormField(
                        label = "Phone Number",
                        value = uiState.phone,
                        onValueChange = onPhoneChange,
                        placeholder = "10-digit mobile number",
                        leadingIcon = Icons.Default.Phone,
                        keyboardType = KeyboardType.Phone
                    )

                    CompactFormField(
                        label = "Password",
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        placeholder = "At least 6 characters",
                        leadingIcon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )

                    // Password Strength Indicator Pills
                    if (password.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (badgeText, badgeColor) = when (strengthScore) {
                                3 -> "Strong password" to SuccessGreen
                                2 -> "Medium strength" to WarningAmber
                                else -> "Weak password" to MaterialTheme.colorScheme.error
                            }

                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (i in 1..3) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (i <= strengthScore) badgeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            )
                                    )
                                }
                            }

                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Gender Chip Selector Row
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Text(
                            text = "Gender",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Male", "Female", "Other").forEach { gender ->
                                val isSelected = uiState.gender == gender
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onGenderChange(gender)
                                    },
                                    label = {
                                        Text(
                                            text = gender,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    CompactFormField(
                        label = "Current City",
                        value = uiState.city,
                        onValueChange = onCityChange,
                        placeholder = "e.g. Delhi NCR",
                        leadingIcon = Icons.Default.LocationOn
                    )

                    AnimatedVisibility(
                        visible = uiState.role == "student",
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        CompactFormField(
                            label = "College / University",
                            value = uiState.college,
                            onValueChange = onCollegeChange,
                            placeholder = "e.g. IIT Delhi / Amity",
                            leadingIcon = Icons.Default.School
                        )
                    }
                }
            }

            // Error Display Banner
            uiState.errorMessage?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary CTA Button (Compact Height 48dp)
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRegister()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Create Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Already have an account link
            Row(
                modifier = Modifier
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Log In",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateBack() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Android Studio Live Previews

@Preview(name = "Light Mode - Student", showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenStudentPreview() {
    StayBuddyTheme {
        RegisterContent(
            uiState = RegisterUiState(
                role = "student",
                name = "Rahul Sharma",
                email = "rahul@example.com",
                phone = "9876543210",
                password = "Password1",
                gender = "Male",
                city = "Delhi NCR",
                college = "IIT Delhi"
            ),
            onRoleChange = {},
            onNameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onGenderChange = {},
            onCityChange = {},
            onCollegeChange = {},
            onRegister = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Light Mode - Host Owner", showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenOwnerPreview() {
    StayBuddyTheme {
        RegisterContent(
            uiState = RegisterUiState(
                role = "owner",
                name = "Vikram Singh",
                email = "vikram@staybuddy.in",
                phone = "9812345678",
                password = "Secret",
                gender = "Male",
                city = "Gurugram"
            ),
            onRoleChange = {},
            onNameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onGenderChange = {},
            onCityChange = {},
            onCollegeChange = {},
            onRegister = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Dark Mode - Student", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenDarkModePreview() {
    StayBuddyTheme {
        RegisterContent(
            uiState = RegisterUiState(
                role = "student",
                name = "Priya Ananya",
                email = "priya@example.com",
                phone = "9988776655",
                password = "Password123",
                gender = "Female",
                city = "Bengaluru",
                college = "IISc Bengaluru"
            ),
            onRoleChange = {},
            onNameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onGenderChange = {},
            onCityChange = {},
            onCollegeChange = {},
            onRegister = {},
            onNavigateBack = {}
        )
    }
}

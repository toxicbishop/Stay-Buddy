package com.example.staybuddy.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.staybuddy.utils.Constants
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoginSuccess: Boolean = false,
    val role: String = "student",
    val isNewUser: Boolean = false,
    val isPasswordResetEmailSent: Boolean = false,
    // Ban check
    val isBanned: Boolean = false,
    val banReason: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null, successMessage = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null, successMessage = null)
    }

    fun signInWithEmail() {
        val state = _uiState.value
        
        // Basic Validation
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please fill in all fields")
            return
        }
        
        // Email Validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches()) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid email address")
            return
        }
        
        // Password Validation (min 6 chars as per 02_authentication.md)
        if (state.password.length < 6) {
            _uiState.value = state.copy(errorMessage = "Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signInWithEmail(state.email.trim(), state.password)
            result.fold(
                onSuccess = { firebaseUser ->
                    val profileResult = authRepository.getUserFromFirestore(firebaseUser.uid)
                    val existingUser = profileResult.getOrNull()

                    // Check if user is banned
                    if (existingUser != null && !existingUser.isActive) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isBanned = true,
                            banReason = existingUser.banReason
                        )
                        // Sign out the banned user
                        authRepository.signOut()
                        return@fold
                    }

                    // Sync FCM token on login (so notifications work)
                    viewModelScope.launch {
                        authRepository.getAndSyncFcmToken()
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoginSuccess = true,
                        role = existingUser?.role ?: "student"
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Login failed"
                    )
                }
            )
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val credentialManager = CredentialManager.create(context)
                
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(Constants.WEB_CLIENT_ID)
                    .build()

                val request: GetCredentialRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    
                    val authResult = authRepository.signInWithGoogle(idToken)
                    authResult.fold(
                        onSuccess = { firebaseUser ->
                            // Check if user exists in Firestore.
                            val profileResult = authRepository.getUserFromFirestore(firebaseUser.uid)
                            if (profileResult.isSuccess && (profileResult.getOrNull() == null || profileResult.getOrNull()?.name.isNullOrBlank())) {
                                // New user (or incomplete profile) - redirect to FinishRegistration instead of saving directly
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isNewUser = true
                                )
                            } else {
                                val existingUser = profileResult.getOrNull()
                                // Check if user is banned
                                if (existingUser != null && !existingUser.isActive) {
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        isBanned = true,
                                        banReason = existingUser.banReason
                                    )
                                    authRepository.signOut()
                                    return@fold
                                }
                                // Existing user - proceed to Home/Dashboard
                                // Sync FCM token on login
                                viewModelScope.launch {
                                    authRepository.getAndSyncFcmToken()
                                }

                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isLoginSuccess = true,
                                    role = existingUser?.role ?: "student"
                                )
                            }
                        },
                        onFailure = { e ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "Google sign-in failed"
                            )
                        }
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Unexpected credential type"
                    )
                }
            } catch (e: GetCredentialException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Google sign-in canceled or failed"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An error occurred"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetPassword() {
        val state = _uiState.value
        if (state.email.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter your email to reset password")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches()) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid email address")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null, isPasswordResetEmailSent = false)
            val result = authRepository.sendPasswordResetEmail(state.email.trim())
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isPasswordResetEmailSent = true,
                        successMessage = "Password reset email sent. Please check your inbox."
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to send reset email"
                    )
                }
            )
        }
    }

    fun clearPasswordResetState() {
        _uiState.value = _uiState.value.copy(isPasswordResetEmailSent = false, successMessage = null)
    }
}

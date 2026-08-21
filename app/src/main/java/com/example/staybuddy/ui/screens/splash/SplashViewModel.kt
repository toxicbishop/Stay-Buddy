package com.example.staybuddy.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.repository.AuthRepository
import com.example.staybuddy.data.manager.PreferenceManager
import com.example.staybuddy.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class SplashDestination {
    data object Onboarding : SplashDestination()
    data object Login : SplashDestination()
    data object Home : SplashDestination()
    data object OwnerDashboard : SplashDestination()
    data object FinishRegistration : SplashDestination()
    data object MaintenanceMode : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferenceManager: PreferenceManager,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination = _destination.asStateFlow()

    fun checkAuthState() {
        viewModelScope.launch {
            // Check maintenance mode first
            val isMaintenance = checkMaintenanceMode()
            if (isMaintenance) {
                _destination.value = SplashDestination.MaintenanceMode
                return@launch
            }

            val isOnboardingCompleted = preferenceManager.isOnboardingCompleted.first()
            if (!isOnboardingCompleted) {
                _destination.value = SplashDestination.Onboarding
                return@launch
            }

            val currentUser = authRepository.currentUser
            if (currentUser != null) {
                android.util.Log.d("SplashViewModel", "checkAuthState: User logged in, fetching profile")
                val profileResult = authRepository.getUserFromFirestore(currentUser.uid)
                val user = profileResult.getOrNull()
                if (profileResult.isSuccess && (user == null || user.name.isBlank())) {
                    android.util.Log.d("SplashViewModel", "checkAuthState: Profile missing or incomplete, redirecting to FinishRegistration")
                    _destination.value = SplashDestination.FinishRegistration
                    return@launch
                } else if (profileResult.isFailure) {
                    android.util.Log.e("SplashViewModel", "checkAuthState: Profile fetch failed, proceeding to Home anyway")
                }

                // Check if user is banned
                if (user != null && !user.isActive) {
                    android.util.Log.d("SplashViewModel", "checkAuthState: User is banned")
                    authRepository.signOut()
                    _destination.value = SplashDestination.Login
                    return@launch
                }

                val destination = if (user?.role.equals(Constants.ROLE_OWNER, ignoreCase = true)) {
                    SplashDestination.OwnerDashboard
                } else {
                    SplashDestination.Home
                }
                android.util.Log.d("SplashViewModel", "checkAuthState: Navigating to $destination")
                _destination.value = destination
            } else {
                android.util.Log.d("SplashViewModel", "checkAuthState: No user, navigating to Login")
                _destination.value = SplashDestination.Login
            }
        }
    }

    private suspend fun checkMaintenanceMode(): Boolean {
        return try {
            val doc = firestore.collection("config").document("remote_config")
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
            doc.getBoolean("maintenance_mode") ?: false
        } catch (e: Exception) {
            false // If can't reach Firestore, don't block the app
        }
    }
}

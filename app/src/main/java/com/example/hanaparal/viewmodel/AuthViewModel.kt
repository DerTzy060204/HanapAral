package com.example.hanaparal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanaparal.auth.FirebaseAuthState
import com.example.hanaparal.auth.SignInResult
import com.example.hanaparal.data.repository.AuthRepository
import com.example.hanaparal.data.repository.FirestoreRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<FirebaseAuthState>(FirebaseAuthState.Idle)
    val authState: StateFlow<FirebaseAuthState> = _authState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authStateFlow()
                .flatMapLatest { user ->
                    Log.d("AuthViewModel", "Auth change detected: User=${user?.uid}")
                    if (user == null) {
                        flowOf(FirebaseAuthState.Unauthenticated)
                    } else {
                        // Immediately check for profile to avoid "Authenticated" flicker
                        firestoreRepository.observeProfile(user.uid)
                            .map { profile ->
                                if (profile != null && profile.name.isNotBlank() && profile.course.isNotBlank()) {
                                    Log.d("AuthViewModel", "Profile found: Authenticated")
                                    FirebaseAuthState.Authenticated(user)
                                } else {
                                    Log.d("AuthViewModel", "Profile missing: NeedsProfile")
                                    FirebaseAuthState.NeedsProfile(user)
                                }
                            }
                            .catch { e ->
                                Log.e("AuthViewModel", "Profile check failed: ${e.message}")
                                emit(FirebaseAuthState.NeedsProfile(user))
                            }
                    }
                }
                .collect { state ->
                    _authState.value = state
                }
        }
    }

    fun onSignInResult(result: SignInResult) {
        if (result.data == null) {
            _authState.value = FirebaseAuthState.Error(result.errorMessage ?: "Sign-in failed")
        } else {
            _authState.value = FirebaseAuthState.Loading
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = FirebaseAuthState.Loading
            authRepository.signInWithEmail(email, password).onFailure { e ->
                _authState.value = FirebaseAuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = FirebaseAuthState.Loading
            authRepository.signUpWithEmail(email, password).onFailure { e ->
                _authState.value = FirebaseAuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun signOut(onSignOutComplete: () -> Unit = {}) {
        authRepository.signOut()
        _authState.value = FirebaseAuthState.Unauthenticated
        onSignOutComplete()
    }

    fun resetState() { _authState.value = FirebaseAuthState.Idle }
}

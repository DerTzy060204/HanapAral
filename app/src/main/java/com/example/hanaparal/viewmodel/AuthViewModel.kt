package com.example.hanaparal.viewmodel

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
                    if (user == null) {
                        flowOf(FirebaseAuthState.Unauthenticated)
                    } else {
                        // Observe the profile in real-time.
                        // If it's created or updated, this flow will emit a new state.
                        firestoreRepository.observeProfile(user.uid).map { profile ->
                            if (profile != null && profile.name.isNotBlank() && profile.course.isNotBlank()) {
                                FirebaseAuthState.Authenticated(user)
                            } else {
                                FirebaseAuthState.NeedsProfile(user)
                            }
                        }
                    }
                }
                .collect { state ->
                    _authState.value = state
                }
        }
    }

    /** Called after the Google One-Tap flow completes to finalize sign-in. */
    fun onSignInResult(result: SignInResult) {
        if (result.data == null) {
            _authState.value = FirebaseAuthState.Error(result.errorMessage ?: "Unknown sign-in error")
        } else {
            // State will be updated automatically by the flatMapLatest observer above
            _authState.value = FirebaseAuthState.Loading
        }
    }

    fun signOut(onSignOutComplete: () -> Unit = {}) {
        authRepository.signOut()
        _authState.value = FirebaseAuthState.Unauthenticated
        onSignOutComplete()
    }

    fun resetState() {
        _authState.value = FirebaseAuthState.Idle
    }
}

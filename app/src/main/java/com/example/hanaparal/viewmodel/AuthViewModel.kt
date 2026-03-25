package com.example.hanaparal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanaparal.auth.FirebaseAuthState
import com.example.hanaparal.auth.SignInResult
import com.example.hanaparal.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<FirebaseAuthState>(FirebaseAuthState.Idle)
    val authState: StateFlow<FirebaseAuthState> = _authState.asStateFlow()

    // Reflects the current sign-in state as soon as the ViewModel is created
    init {
        viewModelScope.launch {
            authRepository.authStateFlow().collect { user ->
                _authState.value = if (user != null) {
                    FirebaseAuthState.Authenticated(user)
                } else {
                    FirebaseAuthState.Unauthenticated
                }
            }
        }
    }

    /** Called after the Google One-Tap flow completes to finalize sign-in. */
    fun onSignInResult(result: SignInResult) {
        val user = authRepository.currentUser
        _authState.value = if (result.data != null && user != null) {
            FirebaseAuthState.Authenticated(user)
        } else {
            FirebaseAuthState.Error(result.errorMessage ?: "Unknown sign-in error")
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

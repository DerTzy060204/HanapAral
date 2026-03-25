package com.example.hanaparal.auth

import com.google.firebase.auth.FirebaseUser

sealed class FirebaseAuthState {
    object Idle : FirebaseAuthState()
    object Loading : FirebaseAuthState()
    data class Authenticated(val user: FirebaseUser) : FirebaseAuthState()
    object Unauthenticated : FirebaseAuthState()
    data class Error(val message: String) : FirebaseAuthState()
}

package com.example.hanaparal.data.repository

import com.example.hanaparal.data.source.remote.FirebaseAuthDataSource
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val dataSource: FirebaseAuthDataSource = FirebaseAuthDataSource()
) {
    val currentUser: FirebaseUser? get() = dataSource.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = dataSource.authStateFlow()

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser?> {
        return try {
            Result.success(dataSource.signInWithGoogle(idToken))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser?> {
        return try {
            Result.success(dataSource.createUserWithEmail(email, password))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser?> {
        return try {
            Result.success(dataSource.signInWithEmail(email, password))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() = dataSource.signOut()
}

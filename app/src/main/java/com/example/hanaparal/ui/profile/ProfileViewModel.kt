package com.example.hanaparal.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanaparal.data.model.StudentProfile
import com.example.hanaparal.data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    object Saved : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Reactive profile stream — auto-updates when Firestore changes
    val profile: StateFlow<StudentProfile?> = repository.observeProfile(userId)
        .catch { e ->
            Log.e("ProfileViewModel", "Error observing profile: ${e.message}")
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveProfile(profile: StudentProfile) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            repository.saveProfile(profile.copy(userId = userId)).fold(
                onSuccess = { _uiState.value = ProfileUiState.Saved },
                onFailure = { e ->
                    _uiState.value = ProfileUiState.Error(e.message ?: "Failed to save profile")
                }
            )
        }
    }

    fun resetState() { _uiState.value = ProfileUiState.Idle }
}

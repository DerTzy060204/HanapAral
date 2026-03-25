package com.example.hanaparal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanaparal.data.repository.FirestoreRepository
import com.example.hanaparal.data.repository.RemoteConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class AppConfig(
    val globalAnnouncementHeader: String = "Welcome to HanapAral!",
    val maxMembersPerGroup: Int = 10,
    val isGroupCreationEnabled: Boolean = true,
    val isJoiningGroupsEnabled: Boolean = true,
    val superuserEmails: List<String> = emptyList()
)

class MainViewModel(
    private val remoteConfigRepository: RemoteConfigRepository = RemoteConfigRepository(),
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _appConfig = MutableStateFlow(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Initial fetch from Remote Config
        fetchRemoteConfig(showLoading = true)

        // Then start observing Firestore for overrides
        observeFirestoreConfig()
    }

    private fun observeFirestoreConfig() {
        viewModelScope.launch {
            firestoreRepository.observeAppConfig()
                .catch { e ->
                    Log.e("MainViewModel", "Error observing Firestore config: ${e.message}")
                    // Fallback to Remote Config values if Firestore fails (e.g. not logged in yet)
                }
                .collectLatest { firestoreConfig ->
                    if (firestoreConfig != null) {
                        _appConfig.value = firestoreConfig
                    }
                }
        }
    }

    fun fetchRemoteConfig(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) _isLoading.value = true

            try {
                remoteConfigRepository.fetchAndActivate()

                _appConfig.value = AppConfig(
                    globalAnnouncementHeader = remoteConfigRepository.getGlobalAnnouncementHeader(),
                    maxMembersPerGroup = remoteConfigRepository.getMaxMembersPerGroup(),
                    isGroupCreationEnabled = remoteConfigRepository.isGroupCreationEnabled(),
                    isJoiningGroupsEnabled = remoteConfigRepository.isJoiningGroupsEnabled(),
                    superuserEmails = remoteConfigRepository.getSuperuserEmails()
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching Remote Config: ${e.message}")
            } finally {
                if (showLoading) _isLoading.value = false
            }
        }
    }

    fun updateConfig(newConfig: AppConfig, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            firestoreRepository.updateAppConfig(newConfig).fold(
                onSuccess = { onComplete(true) },
                onFailure = { onComplete(false) }
            )
        }
    }
}

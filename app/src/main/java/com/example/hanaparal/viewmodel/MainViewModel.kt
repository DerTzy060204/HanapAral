package com.example.hanaparal.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanaparal.data.NetworkObservers
import com.example.hanaparal.data.NetworkStatus
import com.example.hanaparal.data.repository.FirestoreRepository
import com.example.hanaparal.data.repository.RemoteConfigRepository
import com.google.firebase.firestore.PropertyName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppConfig(
    @get:PropertyName("global_announcement_header")
    @set:PropertyName("global_announcement_header")
    var globalAnnouncementHeader: String = "Welcome to HanapAral!",

    @get:PropertyName("max_members_per_group")
    @set:PropertyName("max_members_per_group")
    var maxMembersPerGroup: Int = 10,

    @get:PropertyName("is_group_creation_enabled")
    @set:PropertyName("is_group_creation_enabled")
    var isGroupCreationEnabled: Boolean = true,

    @get:PropertyName("is_joining_groups_enabled")
    @set:PropertyName("is_joining_groups_enabled")
    var isJoiningGroupsEnabled: Boolean = true,

    @get:PropertyName("enable_biometric")
    @set:PropertyName("enable_biometric")
    var isBiometricEnabled: Boolean = true,

    @get:PropertyName("maintenance_mode")
    @set:PropertyName("maintenance_mode")
    var isMaintenanceMode: Boolean = false,

    @get:PropertyName("featured_subject")
    @set:PropertyName("featured_subject")
    var featuredSubject: String = "General",

    @get:PropertyName("superuser_emails")
    @set:PropertyName("superuser_emails")
    var superuserEmails: List<String> = emptyList()
)

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val remoteConfigRepository: RemoteConfigRepository = RemoteConfigRepository(),
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : AndroidViewModel(application) {

    private val networkObservers = NetworkObservers(application)
    val networkStatus: StateFlow<NetworkStatus> = networkObservers.networkStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NetworkStatus.Available
        )

    private val _remoteConfig = MutableStateFlow(AppConfig())
    private val _firestoreConfig = MutableStateFlow<AppConfig?>(null)

    // Combine Remote Config (defaults) with Firestore (real-time overrides)
    // Firestore always wins if a value is present, but permissions are combined using AND.
    val appConfig: StateFlow<AppConfig> = combine(_remoteConfig, _firestoreConfig) { rc, fs ->
        if (fs == null) return@combine rc
        
        AppConfig(
            globalAnnouncementHeader = fs.globalAnnouncementHeader,
            maxMembersPerGroup = fs.maxMembersPerGroup,
            // Permission Logic: Both Remote Config AND Firestore must be true
            isGroupCreationEnabled = rc.isGroupCreationEnabled && fs.isGroupCreationEnabled,
            isJoiningGroupsEnabled = rc.isJoiningGroupsEnabled && fs.isJoiningGroupsEnabled,
            isBiometricEnabled = rc.isBiometricEnabled && fs.isBiometricEnabled,
            // Maintenance logic: Either one can trigger it
            isMaintenanceMode = rc.isMaintenanceMode || fs.isMaintenanceMode,
            featuredSubject = fs.featuredSubject,
            superuserEmails = (rc.superuserEmails + fs.superuserEmails).distinct()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppConfig()
    )

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // 1. Initial fetch from Remote Config (sets base values)
        fetchRemoteConfig(showLoading = true)

        // 2. Start observing Firestore for overrides (sets real-time values)
        observeFirestoreConfig()

        // 3. Set up Real-time Remote Config listener
        remoteConfigRepository.setRealTimeUpdateListener {
            Log.d("MainViewModel", "Real-time update received from Remote Config")
            fetchRemoteConfig(showLoading = false)
        }
    }

    private fun observeFirestoreConfig() {
        viewModelScope.launch {
            firestoreRepository.observeAppConfig()
                .catch { e ->
                    Log.e("MainViewModel", "Error observing Firestore config: ${e.message}")
                }
                .collect { firestoreConfig ->
                    if (firestoreConfig != null) {
                        Log.d("MainViewModel", "Firestore override received: $firestoreConfig")
                        _firestoreConfig.value = firestoreConfig
                    }
                }
        }
    }

    fun fetchRemoteConfig(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) _isLoading.value = true
            try {
                remoteConfigRepository.fetchAndActivate()
                _remoteConfig.value = AppConfig(
                    globalAnnouncementHeader = remoteConfigRepository.getWelcomeMessage(),
                    maxMembersPerGroup = remoteConfigRepository.getMaxGroupSize(),
                    isGroupCreationEnabled = remoteConfigRepository.isGroupCreationEnabled(),
                    isJoiningGroupsEnabled = remoteConfigRepository.isJoiningGroupsEnabled(),
                    isBiometricEnabled = remoteConfigRepository.isBiometricEnabled(),
                    isMaintenanceMode = remoteConfigRepository.isMaintenanceMode(),
                    featuredSubject = remoteConfigRepository.getFeaturedSubject(),
                    superuserEmails = remoteConfigRepository.getSuperuserEmails()
                )
                Log.d("MainViewModel", "Remote Config base updated: ${_remoteConfig.value}")
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
                onSuccess = {
                    Log.d("MainViewModel", "Successfully updated config in Firestore")
                    onComplete(true)
                },
                onFailure = {
                    Log.e("MainViewModel", "Update failed: ${it.message}")
                    onComplete(false)
                }
            )
        }
    }
}

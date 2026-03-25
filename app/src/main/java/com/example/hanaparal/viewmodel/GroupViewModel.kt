package com.example.hanaparal.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.data.model.StudyGroup
import com.example.hanaparal.data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class GroupUiState {
    object Idle : GroupUiState()
    object Loading : GroupUiState()
    object Success : GroupUiState()
    data class Error(val message: String) : GroupUiState()
}

class GroupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FirestoreRepository = FirestoreRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _userIdFlow = MutableStateFlow(auth.currentUser?.uid ?: "")

    private val _uiState = MutableStateFlow<GroupUiState>(GroupUiState.Idle)
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    val allGroups: StateFlow<List<StudyGroup>> = repository.observeAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val myGroups: StateFlow<List<StudyGroup>> = _userIdFlow
        .flatMapLatest { uid ->
            if (uid.isEmpty()) flowOf(emptyList())
            else repository.observeUserGroups(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _userIdFlow.value = firebaseAuth.currentUser?.uid ?: ""
        }
    }

    fun joinGroup(groupId: String, groupName: String = "the group") {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = GroupUiState.Loading
            repository.joinGroup(groupId, uid).fold(
                onSuccess = {
                    FirebaseMessaging.getInstance().subscribeToTopic("group_$groupId")
                    _uiState.value = GroupUiState.Success
                    showLocalJoinNotification(groupName)
                },
                onFailure = { e ->
                    _uiState.value = GroupUiState.Error(e.message ?: "Failed to join group")
                }
            )
        }
    }

    private fun showLocalJoinNotification(groupName: String) {
        val context = getApplication<Application>()
        val channelId = "group_join_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Group Join Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Group Joined!")
            .setContentText("You are now a member of $groupName.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun createGroup(group: StudyGroup, onSuccess: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = GroupUiState.Loading
            val userProfile = repository.getProfile(uid).getOrNull()
            val adminName = userProfile?.name ?: auth.currentUser?.displayName ?: "Student"
            val groupWithAdmin = group.copy(adminId = uid, adminName = adminName, memberIds = listOf(uid))
            repository.createGroup(groupWithAdmin).fold(
                onSuccess = { groupId ->
                    FirebaseMessaging.getInstance().subscribeToTopic("group_$groupId")
                    _uiState.value = GroupUiState.Success
                    onSuccess(groupId)
                },
                onFailure = { e -> _uiState.value = GroupUiState.Error(e.message ?: "Failed") }
            )
        }
    }

    fun updateGroup(group: StudyGroup) {
        viewModelScope.launch {
            _uiState.value = GroupUiState.Loading
            repository.updateGroup(group).fold(
                onSuccess = { _uiState.value = GroupUiState.Success },
                onFailure = { e -> _uiState.value = GroupUiState.Error(e.message ?: "Failed to update") }
            )
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.value = GroupUiState.Loading
            repository.deleteGroup(groupId).fold(
                onSuccess = {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("group_$groupId")
                    _uiState.value = GroupUiState.Success
                },
                onFailure = { e -> _uiState.value = GroupUiState.Error(e.message ?: "Failed to delete") }
            )
        }
    }

    fun leaveGroup(groupId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = GroupUiState.Loading
            repository.leaveGroup(groupId, uid).fold(
                onSuccess = {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("group_$groupId")
                    _uiState.value = GroupUiState.Success
                },
                onFailure = { e -> _uiState.value = GroupUiState.Error(e.message ?: "Failed") }
            )
        }
    }

    fun getAnnouncementsFlow(groupId: String): StateFlow<List<Announcement>> =
        repository.observeAnnouncements(groupId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun postAnnouncement(announcement: Announcement) {
        viewModelScope.launch { repository.postAnnouncement(announcement) }
    }

    fun resetState() { _uiState.value = GroupUiState.Idle }
}

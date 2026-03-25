package com.example.hanaparal.data.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object RemoteConfigKeys {
    // Matched to your Firebase Console Screenshot
    const val GLOBAL_ANNOUNCEMENT_HEADER = "welcome_message"
    const val MAX_MEMBERS_PER_GROUP = "max_group_size"
    const val ENABLE_GROUP_CREATION = "enable_group_creation"
    const val SUPERUSER_EMAILS = "superuser_emails"
    const val ALLOW_JOINING_GROUPS = "allow_joining_groups"
}

class RemoteConfigRepository {
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    init {
        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(settings)

        remoteConfig.setDefaultsAsync(
            mapOf(
                RemoteConfigKeys.GLOBAL_ANNOUNCEMENT_HEADER to "Welcome to HanapAral!",
                RemoteConfigKeys.MAX_MEMBERS_PER_GROUP to 10L,
                RemoteConfigKeys.ENABLE_GROUP_CREATION to true,
                RemoteConfigKeys.SUPERUSER_EMAILS to "admin@hanaparal.com,remedioelder2@gmail.com",
                RemoteConfigKeys.ALLOW_JOINING_GROUPS to true
            )
        )
    }

    suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            false
        }
    }

    fun getGlobalAnnouncementHeader(): String =
        remoteConfig.getString(RemoteConfigKeys.GLOBAL_ANNOUNCEMENT_HEADER)

    fun getMaxMembersPerGroup(): Int =
        remoteConfig.getLong(RemoteConfigKeys.MAX_MEMBERS_PER_GROUP).toInt()

    fun isGroupCreationEnabled(): Boolean =
        remoteConfig.getBoolean(RemoteConfigKeys.ENABLE_GROUP_CREATION)

    fun isJoiningGroupsEnabled(): Boolean =
        remoteConfig.getBoolean(RemoteConfigKeys.ALLOW_JOINING_GROUPS)

    fun getSuperuserEmails(): List<String> =
        remoteConfig.getString(RemoteConfigKeys.SUPERUSER_EMAILS)
            .split(",")
            .map { it.trim().lowercase() }
}

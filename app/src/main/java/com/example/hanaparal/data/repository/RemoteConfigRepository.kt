package com.example.hanaparal.data.repository

import android.util.Log
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object RemoteConfigKeys {
    // Matched to your Firebase Console Screenshot
    const val WELCOME_MESSAGE = "welcome_message"
    const val MAX_GROUP_SIZE = "max_group_size"
    const val ENABLE_GROUP_CREATION = "enable_group_creation"
    const val ALLOW_JOINING_GROUPS = "allow_joining_groups"
    const val ENABLE_BIOMETRIC = "enable_biometric"
    const val MAINTENANCE_MODE = "maintenance_mode"
    const val FEATURED_SUBJECT = "featured_subject"
    const val SUPERUSER_EMAILS = "superuser_emails"
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
                RemoteConfigKeys.WELCOME_MESSAGE to "Welcome to HanapAral!",
                RemoteConfigKeys.MAX_GROUP_SIZE to 10L,
                RemoteConfigKeys.ENABLE_GROUP_CREATION to true,
                RemoteConfigKeys.ALLOW_JOINING_GROUPS to true,
                RemoteConfigKeys.ENABLE_BIOMETRIC to true,
                RemoteConfigKeys.MAINTENANCE_MODE to false,
                RemoteConfigKeys.FEATURED_SUBJECT to "General",
                RemoteConfigKeys.SUPERUSER_EMAILS to "admin@hanaparal.com"
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

    /** Sets up a real-time listener for Remote Config changes */
    fun setRealTimeUpdateListener(onUpdate: () -> Unit) {
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Log.d("RemoteConfig", "Updated keys: " + configUpdate.updatedKeys)
                // Activate the new config and then trigger the UI update
                remoteConfig.activate().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onUpdate()
                    }
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.e("RemoteConfig", "Config update error", error)
            }
        })
    }

    fun getWelcomeMessage(): String =
        remoteConfig.getString(RemoteConfigKeys.WELCOME_MESSAGE)

    fun getMaxGroupSize(): Int =
        remoteConfig.getLong(RemoteConfigKeys.MAX_GROUP_SIZE).toInt()

    fun isGroupCreationEnabled(): Boolean =
        remoteConfig.getBoolean(RemoteConfigKeys.ENABLE_GROUP_CREATION)

    fun isJoiningGroupsEnabled(): Boolean =
        remoteConfig.getBoolean(RemoteConfigKeys.ALLOW_JOINING_GROUPS)

    fun isBiometricEnabled(): Boolean =
        remoteConfig.getBoolean(RemoteConfigKeys.ENABLE_BIOMETRIC)

    fun isMaintenanceMode(): Boolean =
        remoteConfig.getBoolean(RemoteConfigKeys.MAINTENANCE_MODE)

    fun getFeaturedSubject(): String =
        remoteConfig.getString(RemoteConfigKeys.FEATURED_SUBJECT)

    fun getSuperuserEmails(): List<String> =
        remoteConfig.getString(RemoteConfigKeys.SUPERUSER_EMAILS)
            .split(",")
            .map { it.trim().lowercase() }
}

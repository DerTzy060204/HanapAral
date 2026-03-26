package com.example.hanaparal.data.repository

import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.data.model.StudentProfile
import com.example.hanaparal.data.model.StudyGroup
import com.example.hanaparal.data.source.remote.FirebaseFirestoreDataSource
import com.example.hanaparal.viewmodel.AppConfig
import kotlinx.coroutines.flow.Flow

class FirestoreRepository(
    private val dataSource: FirebaseFirestoreDataSource = FirebaseFirestoreDataSource()
) {
    // ── Profile ──────────────────────────────────────────────────────────

    suspend fun saveProfile(profile: StudentProfile): Result<Unit> {
        return try {
            dataSource.saveProfile(profile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(userId: String): Result<StudentProfile?> {
        return try {
            Result.success(dataSource.getProfile(userId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeProfile(userId: String): Flow<StudentProfile?> =
        dataSource.observeProfile(userId)

    // ── Study Groups ─────────────────────────────────────────────────────

    suspend fun createGroup(group: StudyGroup): Result<String> {
        return try {
            Result.success(dataSource.createGroup(group))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGroup(group: StudyGroup): Result<Unit> {
        return try {
            dataSource.updateGroup(group)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            dataSource.deleteGroup(groupId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAllGroups(): Flow<List<StudyGroup>> = dataSource.observeAllGroups()

    fun observeUserGroups(userId: String): Flow<List<StudyGroup>> =
        dataSource.observeUserGroups(userId)

    suspend fun joinGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            dataSource.joinGroup(groupId, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            dataSource.leaveGroup(groupId, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Announcements ─────────────────────────────────────────────────────

    suspend fun postAnnouncement(announcement: Announcement): Result<Unit> {
        return try {
            dataSource.postAnnouncement(announcement)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAnnouncements(groupId: String): Flow<List<Announcement>> =
        dataSource.observeAnnouncements(groupId)

    // ── App Config ────────────────────────────────────────────────────────

    suspend fun updateAppConfig(config: AppConfig): Result<Unit> {
        return try {
            dataSource.updateAppConfig(config)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAppConfig(): Flow<AppConfig?> = dataSource.observeAppConfig()
}

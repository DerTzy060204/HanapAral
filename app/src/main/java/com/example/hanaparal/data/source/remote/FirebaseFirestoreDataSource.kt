package com.example.hanaparal.data.source.remote

import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.data.model.StudentProfile
import com.example.hanaparal.data.model.StudyGroup
import com.example.hanaparal.viewmodel.AppConfig
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class FirebaseFirestoreDataSource {
    private val db = Firebase.firestore

    // ──────────────────────────── Profile ─────────────────────────────

    suspend fun saveProfile(profile: StudentProfile) {
        db.collection("users").document(profile.userId).set(profile).await()
    }

    suspend fun getProfile(userId: String): StudentProfile? {
        val snapshot = db.collection("users").document(userId).get().await()
        return if (snapshot.exists()) snapshot.toObject(StudentProfile::class.java) else null
    }

    fun observeProfile(userId: String): Flow<StudentProfile?> = callbackFlow {
        val ref = db.collection("users").document(userId)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snapshot?.toObject(StudentProfile::class.java))
        }
        awaitClose { listener.remove() }
    }

    fun observeMembers(memberIds: List<String>): Flow<List<StudentProfile>> {
        if (memberIds.isEmpty()) return flowOf(emptyList())

        return callbackFlow {
            // Firestore whereIn limit is typically 10 for older versions or 30 for newer ones.
            // StudyGroup has maxMembers: Int = 10, so this is safe.
            val listener = db.collection("users")
                .whereIn(FieldPath.documentId(), memberIds)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    val profiles = snapshot?.toObjects(StudentProfile::class.java) ?: emptyList()
                    trySend(profiles)
                }
            awaitClose { listener.remove() }
        }
    }

    // ──────────────────────────── Study Groups ─────────────────────────

    suspend fun createGroup(group: StudyGroup): String {
        val ref = db.collection("groups").document()
        val withId = group.copy(groupId = ref.id)
        ref.set(withId).await()
        return ref.id
    }

    suspend fun updateGroup(group: StudyGroup) {
        db.collection("groups").document(group.groupId).set(group).await()
    }

    suspend fun deleteGroup(groupId: String) {
        db.collection("groups").document(groupId).delete().await()
    }

    fun observeAllGroups(): Flow<List<StudyGroup>> = callbackFlow {
        val listener = db.collection("groups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val groups = snapshot?.toObjects(StudyGroup::class.java) ?: emptyList()
                trySend(groups)
            }
        awaitClose { listener.remove() }
    }

    fun observeUserGroups(userId: String): Flow<List<StudyGroup>> = callbackFlow {
        val listener = db.collection("groups")
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val groups = snapshot?.toObjects(StudyGroup::class.java) ?: emptyList()
                trySend(groups)
            }
        awaitClose { listener.remove() }
    }

    suspend fun joinGroup(groupId: String, userId: String) {
        db.collection("groups").document(groupId)
            .update("memberIds", FieldValue.arrayUnion(userId)).await()
    }

    suspend fun leaveGroup(groupId: String, userId: String) {
        db.collection("groups").document(groupId)
            .update("memberIds", FieldValue.arrayRemove(userId)).await()
    }

    // ──────────────────────────── Announcements ────────────────────────

    suspend fun postAnnouncement(announcement: Announcement) {
        db.collection("groups").document(announcement.groupId)
            .collection("announcements").add(announcement).await()
    }

    suspend fun deleteAnnouncement(groupId: String, announcementId: String) {
        db.collection("groups").document(groupId)
            .collection("announcements").document(announcementId).delete().await()
    }

    fun observeAnnouncements(groupId: String): Flow<List<Announcement>> = callbackFlow {
        val listener = db.collection("groups").document(groupId)
            .collection("announcements")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val items = snapshot?.toObjects(Announcement::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    // ──────────────────────────── App Config ──────────────────────────

    suspend fun updateAppConfig(config: AppConfig) {
        db.collection("config").document("app_settings").set(config).await()
    }

    fun observeAppConfig(): Flow<AppConfig?> = callbackFlow {
        val ref = db.collection("config").document("app_settings")
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snapshot?.toObject(AppConfig::class.java))
        }
        awaitClose { listener.remove() }
    }
}

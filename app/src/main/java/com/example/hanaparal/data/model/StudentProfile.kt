package com.example.hanaparal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class StudentProfile(
    @DocumentId val userId: String = "",
    val name: String = "",
    val email: String = "",
    val course: String = "",
    val yearLevel: Int = 1,
    val photoUrl: String = "",
    val bio: String = "",
    val fcmToken: String = "",
    @ServerTimestamp val updatedAt: Date? = null
)

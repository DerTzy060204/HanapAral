package com.example.hanaparal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Announcement(
    @DocumentId val announcementId: String = "",
    val groupId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    @ServerTimestamp val createdAt: Date? = null
)

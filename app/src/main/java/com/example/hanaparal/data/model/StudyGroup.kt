package com.example.hanaparal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class StudyGroup(
    @DocumentId val groupId: String = "",
    val name: String = "",
    val subject: String = "",
    val description: String = "",
    val adminId: String = "",
    val adminName: String = "",
    val maxMembers: Int = 10,
    val memberIds: List<String> = emptyList(),
    val isOpen: Boolean = true,
    @ServerTimestamp val createdAt: Date? = null
) {
    val memberCount: Int get() = memberIds.size
    val isFull: Boolean get() = memberIds.size >= maxMembers
}

package com.athalukita.privatechat.data.model

import com.google.firebase.Timestamp

enum class MessageType {
    TEXT,
    IMAGE
}

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val type: MessageType = MessageType.TEXT,
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val isViewOnce: Boolean = false,
    val isDeleted: Boolean = false,
    val mediaUrl: String? = null
)

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null
)

data class ChatRoom(
    val chatId: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp = Timestamp.now(),
    val otherUserName: String = "",
    val otherUserPhotoUrl: String? = null,
    val unreadCount: Int = 0
)

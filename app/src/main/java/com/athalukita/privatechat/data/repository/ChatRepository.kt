package com.athalukita.privatechat.data.repository

import com.athalukita.privatechat.data.model.ChatRoom
import com.athalukita.privatechat.data.model.Message
import com.athalukita.privatechat.data.model.MessageType
import com.athalukita.privatechat.utils.SecurityUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    fun getChatId(uid1: String, uid2: String): String {
        return listOf(uid1, uid2).sorted().joinToString("_")
    }

    fun getSharedSecret(chatId: String): ByteArray {
        return SecurityUtils.deriveKey(chatId, chatId.toByteArray())
    }

    fun decryptMessage(encryptedContent: String, chatId: String): String {
        return SecurityUtils.decryptMessage(encryptedContent, getSharedSecret(chatId))
    }

    suspend fun sendMessage(
        receiverId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        isViewOnce: Boolean = false,
        mediaUrl: String? = null
    ) {
        val senderId = auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

        val chatId = getChatId(senderId, receiverId)
        val encryptedContent = SecurityUtils.encryptMessage(content, getSharedSecret(chatId))

        val messageData = hashMapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "content" to encryptedContent,
            "type" to type.name,
            "timestamp" to FieldValue.serverTimestamp(),
            "isRead" to false,
            "isViewOnce" to isViewOnce,
            "isDeleted" to false,
            "mediaUrl" to mediaUrl
        )

        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(messageData)
            .await()

        firestore.collection("chats")
            .document(chatId)
            .set(
                hashMapOf(
                    "participantIds" to listOf(senderId, receiverId).sorted(),
                    "lastMessage" to encryptedContent,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "lastMessageSenderId" to senderId
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Message::class.java)?.copy(id = document.id)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    suspend fun deleteViewOnceMessage(chatId: String, messageId: String) {
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(messageId)
            .update("isDeleted", true)
            .await()
    }

    fun getUserChats(): Flow<List<ChatRoom>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            close(IllegalStateException("User not authenticated"))
            return@callbackFlow
        }

        val listener = firestore.collection("chats")
            .whereArrayContains("participantIds", currentUserId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val chatRooms = snapshot?.documents?.mapNotNull { document ->
                    val participantIds = document.get("participantIds") as? List<*> ?: return@mapNotNull null
                    val ids = participantIds.filterIsInstance<String>()
                    val otherUserId = ids.firstOrNull { it != currentUserId } ?: return@mapNotNull null

                    ChatRoom(
                        chatId = document.id,
                        participantIds = ids,
                        lastMessage = document.getString("lastMessage") ?: "",
                        lastMessageTimestamp = document.getTimestamp("lastMessageTimestamp")
                            ?: com.google.firebase.Timestamp.now(),
                        otherUserName = document.getString("otherUserNames.$otherUserId")
                            ?: otherUserId,
                        otherUserPhotoUrl = document.getString("otherUserPhotos.$otherUserId"),
                        unreadCount = document.getLong("unreadCount.$currentUserId")?.toInt() ?: 0
                    )
                } ?: emptyList()

                trySend(chatRooms)
            }

        awaitClose { listener.remove() }
    }
}

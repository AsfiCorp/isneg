package com.athalukita.privatechat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.athalukita.privatechat.data.model.ChatRoom
import com.athalukita.privatechat.data.model.Message
import com.athalukita.privatechat.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var messagesJob: Job? = null
    private var chatsJob: Job? = null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        loadUserChats()
    }

    fun loadUserChats() {
        chatsJob?.cancel()
        chatsJob = viewModelScope.launch {
            repository.getUserChats()
                .catch { e -> _error.value = e.message }
                .collect { rooms ->
                    _chatRooms.value = rooms.map { room ->
                        room.copy(
                            lastMessage = decryptPreview(room.lastMessage, room.chatId)
                        )
                    }
                }
        }
    }

    fun selectChat(chatId: String) {
        _currentChatId.value = chatId
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessages(chatId)
                .catch { e -> _error.value = e.message }
                .collect { rawMessages ->
                    _messages.value = rawMessages.map { message ->
                        decryptMessageForDisplay(message, chatId)
                    }
                }
        }
    }

    fun sendMessage(content: String) {
        val chatId = _currentChatId.value ?: return
        val userId = currentUserId ?: return
        val receiverId = getOtherParticipantId(chatId, userId) ?: return

        viewModelScope.launch {
            try {
                repository.sendMessage(receiverId, content)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteViewOnceMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            try {
                repository.deleteViewOnceMessage(chatId, messageId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun getOtherParticipantId(chatId: String, currentUserId: String): String? {
        return chatId.split("_").firstOrNull { it != currentUserId }
    }

    private fun decryptMessageForDisplay(message: Message, chatId: String): Message {
        if (message.isDeleted) return message
        return try {
            message.copy(content = repository.decryptMessage(message.content, chatId))
        } catch (e: Exception) {
            message
        }
    }

    private fun decryptPreview(encrypted: String, chatId: String): String {
        return try {
            repository.decryptMessage(encrypted, chatId)
        } catch (e: Exception) {
            encrypted
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
        chatsJob?.cancel()
    }
}

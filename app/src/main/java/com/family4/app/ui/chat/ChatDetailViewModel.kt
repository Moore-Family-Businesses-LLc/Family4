package com.family4.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.ChatDao
import com.family4.app.data.db.entity.ChatMessageEntity
import com.family4.app.security.CryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val cryptoManager: CryptoManager
) : ViewModel() {

    private val _partnerId = MutableStateFlow("")

    /**
     * Decrypted conversation stream — decrypts on the fly before exposing to the UI.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessageUi>> = _partnerId
        .filter { it.isNotEmpty() }
        .flatMapLatest { partnerId ->
            chatDao.getConversation(SELF_ID, partnerId).map { entities ->
                entities.map { entity ->
                    val decrypted = try {
                        cryptoManager.decrypt(entity.encryptedContent, entity.iv)
                    } catch (_: Exception) { "[encrypted]" }
                    ChatMessageUi(
                        id = entity.id,
                        text = decrypted,
                        isMine = entity.isSentByMe,
                        timestamp = entity.timestamp,
                        messageType = entity.messageType,
                        mediaUri = entity.mediaUri
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadConversation(partnerId: String) {
        _partnerId.value = partnerId
        viewModelScope.launch {
            chatDao.markAllRead(SELF_ID, partnerId)
        }
    }

    fun sendMessage(text: String, receiverId: String) = viewModelScope.launch {
        val (ciphertext, iv) = cryptoManager.encrypt(text)
        val message = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = SELF_ID,
            receiverId = receiverId,
            encryptedContent = ciphertext,
            iv = iv,
            isSentByMe = true
        )
        chatDao.insertMessage(message)
        // TODO: transmit over network (WebSocket / FCM)
    }

    companion object {
        private const val SELF_ID = "self"   // Replace with real user ID from auth
    }
}

data class ChatMessageUi(
    val id: String,
    val text: String,
    val isMine: Boolean,
    val timestamp: Long,
    val messageType: String = "TEXT",
    val mediaUri: String? = null
)

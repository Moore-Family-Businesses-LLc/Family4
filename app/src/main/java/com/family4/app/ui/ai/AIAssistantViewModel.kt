package com.family4.app.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.ai.FamilyAIAssistant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIAssistantViewModel @Inject constructor(
    private val assistant: FamilyAIAssistant
) : ViewModel() {

    private val _messages = MutableStateFlow<List<AIMessage>>(emptyList())
    val messages: StateFlow<List<AIMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Sources (grounding URLs) from the last assistant response */
    private val _lastSources = MutableStateFlow<List<String>>(emptyList())
    val lastSources: StateFlow<List<String>> = _lastSources

    fun addBotGreeting() {
        addBotMessage(
            "👋 Hi! I'm **FamilyBot**, your Family4 AI assistant.\n\n" +
            "I can help you:\n" +
            "• Create notes, tasks & calendar events\n" +
            "• Search the internet for current info\n" +
            "• Provide health insights & emergency guidance\n" +
            "• Plan family trips, meals & activities\n" +
            "• Navigate anywhere in the app\n\n" +
            "What can I help you with today?"
        )
    }

    fun sendMessage(text: String) {
        val userMsg = AIMessage(text = text, isUser = true)
        _messages.value = _messages.value + userMsg
        _isLoading.value = true

        viewModelScope.launch {
            // chat() now returns AgentResponse — handle text + sources
            val response = assistant.chat(text)
            addBotMessage(
                text    = response.text,
                sources = response.sources
            )
            _lastSources.value = response.sources
            _isLoading.value = false
        }
    }

    fun setActionDispatcher(dispatcher: FamilyAIAssistant.AgentActionDispatcher) {
        assistant.actionDispatcher = dispatcher
    }

    private fun addBotMessage(text: String, sources: List<String> = emptyList()) {
        val botMsg = AIMessage(text = text, isUser = false, sources = sources)
        _messages.value = _messages.value + botMsg
    }
}

data class AIMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    /** Grounding sources returned by Gemini for this message */
    val sources: List<String> = emptyList()
)

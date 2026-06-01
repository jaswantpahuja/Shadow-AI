package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    application: Application,
    private val repository: ChatRepository
) : AndroidViewModel(application) {

    // List of all chat sessions
    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedSessionId = MutableStateFlow<Long?>(null)
    val selectedSessionId = _selectedSessionId.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _currentPrompt = MutableStateFlow("")
    val currentPrompt = _currentPrompt.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow("1:1")
    val selectedAspectRatio = _selectedAspectRatio.asStateFlow()

    private val _selectedImageSize = MutableStateFlow("1K")
    val selectedImageSize = _selectedImageSize.asStateFlow()

    // Observe messages dynamically when active session shifts
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = _selectedSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessages(sessionId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Automatically select the first session if list is loaded and no selection is set
        viewModelScope.launch {
            repository.allSessions.collect { list ->
                if (_selectedSessionId.value == null && list.isNotEmpty()) {
                    _selectedSessionId.value = list.first().id
                }
            }
        }
    }

    fun selectSession(sessionId: Long) {
        _selectedSessionId.value = sessionId
    }

    fun updatePrompt(text: String) {
        _currentPrompt.value = text
    }

    fun updateAspectRatio(ratio: String) {
        _selectedAspectRatio.value = ratio
    }

    fun updateImageSize(size: String) {
        _selectedImageSize.value = size
    }

    fun startNewSession(onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val defaultName = "Canvas #${System.currentTimeMillis().toString().takeLast(4)}"
            val newId = repository.createSession(defaultName)
            _selectedSessionId.value = newId
            onCreated(newId)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_selectedSessionId.value == sessionId) {
                val remaining = sessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    _selectedSessionId.value = remaining.first().id
                } else {
                    _selectedSessionId.value = null
                }
            }
        }
    }

    fun sendPrompt() {
        val promptText = _currentPrompt.value.trim()
        if (promptText.isEmpty() || _isGenerating.value) return

        val activeSessionId = _selectedSessionId.value

        viewModelScope.launch {
            _isGenerating.value = true
            _currentPrompt.value = ""

            // Resolve target session, or initiate a new one dynamically if none selected
            val targetSessionId = if (activeSessionId == null) {
                val defaultName = if (promptText.length > 20) promptText.take(20) + "..." else promptText
                val newId = repository.createSession(defaultName)
                _selectedSessionId.value = newId
                newId
            } else {
                // If the active session is a blank session, rename it automatically
                val sessionObject = sessions.value.find { it.id == activeSessionId }
                if (sessionObject != null && (sessionObject.title.startsWith("Canvas #") || sessionObject.title.isEmpty())) {
                    val newTitle = if (promptText.length > 20) promptText.take(20) + "..." else promptText
                    repository.updateSessionTitle(activeSessionId, newTitle)
                }
                activeSessionId
            }

            try {
                repository.generateImage(
                    sessionId = targetSessionId,
                    prompt = promptText,
                    aspectRatio = _selectedAspectRatio.value,
                    imageSize = _selectedImageSize.value
                )
            } finally {
                _isGenerating.value = false
            }
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = AppDatabase.getDatabase(application)
                    val repository = ChatRepository(application, database.chatDao())
                    return ChatViewModel(application, repository) as T
                }
            }
    }
}

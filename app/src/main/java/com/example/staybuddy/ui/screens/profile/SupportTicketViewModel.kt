package com.example.staybuddy.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.model.SupportTicket
import com.example.staybuddy.data.repository.SupportRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SupportTicketState {
    object Idle : SupportTicketState()
    object Loading : SupportTicketState()
    data class Success(val displayId: String) : SupportTicketState()
    data class Error(val message: String) : SupportTicketState()
}

@HiltViewModel
class SupportTicketViewModel @Inject constructor(
    private val supportRepository: SupportRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<SupportTicketState>(SupportTicketState.Idle)
    val uiState: StateFlow<SupportTicketState> = _uiState.asStateFlow()

    private val _userTickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    val userTickets: StateFlow<List<SupportTicket>> = _userTickets.asStateFlow()

    init {
        val user = auth.currentUser
        if (user != null) {
            supportRepository.getUserTickets(user.uid).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("SupportTicketVM", "Error loading tickets", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val tickets = snapshot.documents.mapNotNull { it.toObject(SupportTicket::class.java) }
                        .sortedByDescending { it.timestamp }
                    _userTickets.value = tickets
                }
            }
        }
    }

    fun submitTicket(issueType: String, description: String) {
        val user = auth.currentUser
        val userId = user?.uid ?: "unknown"
        val email = user?.email ?: "unknown"

        if (issueType.isBlank() || description.isBlank()) {
            _uiState.value = SupportTicketState.Error("Please fill in all fields.")
            return
        }

        _uiState.value = SupportTicketState.Loading
        viewModelScope.launch {
            val ticket = SupportTicket(
                userId = userId,
                email = email,
                issueType = issueType,
                description = description
            )
            val result = supportRepository.submitTicket(ticket)
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data != null) {
                    val (newTicketId, displayId) = data
                    triggerNotionSync(newTicketId, displayId, ticket)
                    _uiState.value = SupportTicketState.Success(displayId)
                } else {
                    _uiState.value = SupportTicketState.Success("")
                }
            } else {
                _uiState.value = SupportTicketState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = SupportTicketState.Idle
    }

    fun markTicketsAsRead() {
        viewModelScope.launch {
            _userTickets.value.filter { it.unreadByUser }.forEach { ticket ->
                supportRepository.markTicketAsRead(ticket.displayId)
            }
        }
    }

    private fun triggerNotionSync(ticketId: String, displayId: String, ticket: SupportTicket) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://staybuddy-worker.aasavchauhan.workers.dev/api/ticket")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonBody = """
                    {
                        "ticketId": "$ticketId",
                        "displayId": "$displayId",
                        "ticket": {
                            "userId": "${ticket.userId}",
                            "email": "${ticket.email}",
                            "issueType": "${ticket.issueType}",
                            "description": "${ticket.description.replace("\"", "\\\"").replace("\n", "\\n")}",
                            "status": "${ticket.status}"
                        }
                    }
                """.trimIndent()

                connection.outputStream.use { os ->
                    val input = jsonBody.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                connection.responseCode
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

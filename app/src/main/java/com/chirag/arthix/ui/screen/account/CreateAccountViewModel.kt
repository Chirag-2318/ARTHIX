package com.chirag.arthix.ui.screen.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.preferences.AccountPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateAccountUiState(
    val name: String = "",
    val phone: String = "",
    val isSaving: Boolean = false,
    val nameError: String? = null,
)

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val accountPreferences: AccountPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateAccountUiState())
    val uiState: StateFlow<CreateAccountUiState> = _uiState.asStateFlow()

    fun updateName(newName: String) {
        val error = validateName(newName)
        _uiState.update { it.copy(name = newName, nameError = error) }
    }

    fun updatePhone(newPhone: String) {
        _uiState.update { it.copy(phone = newPhone) }
    }

    private fun validateName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Name cannot be empty"
        if (trimmed.length < 2) return "Name is too short"
        if (trimmed.any { it.isDigit() }) return "Name cannot contain numbers"
        return null
    }

    fun canSubmit(): Boolean {
        val state = _uiState.value
        return state.name.trim().isNotEmpty() && validateName(state.name) == null && !state.isSaving
    }

    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        val finalName = if (state.name.trim().isEmpty() || validateName(state.name) != null) "User" else state.name.trim()
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            // Fake-but-intentional 250ms save delay
            delay(250)
            
            accountPreferences.createAccount(finalName, state.phone)
            
            _uiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }
}

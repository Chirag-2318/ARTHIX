package com.chirag.arthix.ui.screen.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.preferences.AccountPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.chirag.arthix.data.ArthixDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AccountUiState(
    val userName: String = "User",
    val phoneNumber: String = "",
    val initials: String = "U",
    val profileAvatar: String? = null,
    val isEditingProfile: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockType: String? = null,
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountPreferences: AccountPreferences,
    private val database: ArthixDatabase,
) : ViewModel() {

    private val _isEditing = MutableStateFlow(false)

    private val appLockState = combine(
        accountPreferences.appLockEnabled,
        accountPreferences.appLockType
    ) { enabled, type ->
        Pair(enabled, type)
    }

    val uiState: StateFlow<AccountUiState> = combine(
        accountPreferences.displayName,
        accountPreferences.phoneNumber,
        accountPreferences.profileAvatar,
        appLockState,
        _isEditing,
    ) { name, phone, avatar, lockState, isEditing ->
        val appLockEnabled = lockState.first
        val appLockType = lockState.second
        val resolvedName = if (name.isNotBlank()) name.trim() else "User"
        val resolvedInitials = resolvedName
            .split(" ")
            .filter { it.isNotBlank() }
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "U" }

        AccountUiState(
            userName = resolvedName,
            phoneNumber = phone.trim(),
            initials = resolvedInitials,
            profileAvatar = avatar,
            isEditingProfile = isEditing,
            appLockEnabled = appLockEnabled,
            appLockType = appLockType,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AccountUiState(),
    )

    fun startEditing() {
        _isEditing.value = true
    }

    fun stopEditing() {
        _isEditing.value = false
    }

    fun updateAvatar(avatar: String?, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            accountPreferences.updateAvatar(avatar)
            onComplete()
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            accountPreferences.setAppLockEnabled(enabled)
        }
    }
    
    fun setAppLock(type: String, hash: String) {
        viewModelScope.launch {
            accountPreferences.setAppLock(type, hash)
        }
    }

    fun saveProfile(name: String, phone: String) {
        viewModelScope.launch {
            accountPreferences.updateProfile(name, phone)
            _isEditing.value = false
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            accountPreferences.signOut()
            onComplete()
        }
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
            accountPreferences.clearAll()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}

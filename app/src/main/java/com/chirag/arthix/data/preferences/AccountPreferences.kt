package com.chirag.arthix.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.accountDataStore by preferencesDataStore(name = "account_prefs")

@Singleton
class AccountPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val ACCOUNT_CREATED = booleanPreferencesKey("account_created")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val PHONE_NUMBER = stringPreferencesKey("phone_number")
        val COACH_MARK_DISMISSED = booleanPreferencesKey("coach_mark_dismissed")
    }

    val isAccountCreated: Flow<Boolean> = context.accountDataStore.data
        .map { prefs -> prefs[Keys.ACCOUNT_CREATED] ?: false }

    val displayName: Flow<String> = context.accountDataStore.data
        .map { prefs -> prefs[Keys.DISPLAY_NAME] ?: "" }
        
    val phoneNumber: Flow<String> = context.accountDataStore.data
        .map { prefs -> prefs[Keys.PHONE_NUMBER] ?: "" }

    val coachMarkDismissed: Flow<Boolean> = context.accountDataStore.data
        .map { prefs -> prefs[Keys.COACH_MARK_DISMISSED] ?: false }

    suspend fun dismissCoachMark() {
        context.accountDataStore.edit { prefs ->
            prefs[Keys.COACH_MARK_DISMISSED] = true
        }
    }

    suspend fun createAccount(name: String, phone: String = "") {
        context.accountDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = name.trim()
            if (phone.isNotBlank()) {
                prefs[Keys.PHONE_NUMBER] = phone.trim()
            }
            prefs[Keys.ACCOUNT_CREATED] = true
        }
    }

    suspend fun updateProfile(name: String, phone: String = "") {
        context.accountDataStore.edit { prefs ->
            if (name.isNotBlank()) {
                prefs[Keys.DISPLAY_NAME] = name.trim()
            }
            if (phone.isNotBlank()) {
                prefs[Keys.PHONE_NUMBER] = phone.trim()
            }
        }
    }

    suspend fun signOut() {
        context.accountDataStore.edit { prefs ->
            prefs[Keys.ACCOUNT_CREATED] = false
            prefs.remove(Keys.DISPLAY_NAME)
            prefs.remove(Keys.PHONE_NUMBER)
        }
    }

    suspend fun clearAll() {
        context.accountDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

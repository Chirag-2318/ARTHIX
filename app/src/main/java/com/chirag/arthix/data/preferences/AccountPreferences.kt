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
        val PROFILE_AVATAR = stringPreferencesKey("profile_avatar")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_TYPE = stringPreferencesKey("app_lock_type")
        val APP_LOCK_HASH = stringPreferencesKey("app_lock_hash")
    }

    val isAccountCreated: Flow<Boolean> = context.accountDataStore.data
        .map { prefs -> prefs[Keys.ACCOUNT_CREATED] ?: false }

    val displayName: Flow<String> = context.accountDataStore.data
        .map { prefs -> prefs[Keys.DISPLAY_NAME] ?: "" }
        
    val phoneNumber: Flow<String> = context.accountDataStore.data
        .map { prefs -> prefs[Keys.PHONE_NUMBER] ?: "" }

    val coachMarkDismissed: Flow<Boolean> = context.accountDataStore.data
        .map { prefs -> prefs[Keys.COACH_MARK_DISMISSED] ?: false }

    val profileAvatar: Flow<String?> = context.accountDataStore.data
        .map { prefs -> prefs[Keys.PROFILE_AVATAR] }

    val appLockEnabled: Flow<Boolean> = context.accountDataStore.data
        .map { prefs -> prefs[Keys.APP_LOCK_ENABLED] ?: false }

    val appLockType: Flow<String?> = context.accountDataStore.data
        .map { prefs -> prefs[Keys.APP_LOCK_TYPE] }

    val appLockHash: Flow<String?> = context.accountDataStore.data
        .map { prefs -> prefs[Keys.APP_LOCK_HASH] }

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

    suspend fun updateAvatar(avatar: String?) {
        context.accountDataStore.edit { prefs ->
            if (avatar == null) {
                prefs.remove(Keys.PROFILE_AVATAR)
            } else {
                prefs[Keys.PROFILE_AVATAR] = avatar
            }
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.accountDataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_ENABLED] = enabled
            if (!enabled) {
                prefs.remove(Keys.APP_LOCK_TYPE)
                prefs.remove(Keys.APP_LOCK_HASH)
            }
        }
    }

    suspend fun setAppLock(type: String, hash: String) {
        context.accountDataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_TYPE] = type
            prefs[Keys.APP_LOCK_HASH] = hash
            prefs[Keys.APP_LOCK_ENABLED] = true
        }
    }

    suspend fun signOut() {
        context.accountDataStore.edit { prefs ->
            prefs[Keys.ACCOUNT_CREATED] = false
            prefs.remove(Keys.DISPLAY_NAME)
            prefs.remove(Keys.PHONE_NUMBER)
            prefs.remove(Keys.PROFILE_AVATAR)
        }
    }

    suspend fun clearAll() {
        context.accountDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

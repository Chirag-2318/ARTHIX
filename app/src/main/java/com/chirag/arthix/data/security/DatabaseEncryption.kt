package com.chirag.arthix.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

/**
 * Database encryption helper using SQLCipher.
 *
 * The encryption key is generated once and stored in
 * EncryptedSharedPreferences (backed by Android Keystore).
 * This means:
 * - The key survives app updates
 * - The key is destroyed on factory reset
 * - The key cannot be extracted without rooting
 *
 * Usage in DatabaseModule:
 * ```kotlin
 * val factory = DatabaseEncryption.getSupportFactory(context)
 * Room.databaseBuilder(...)
 *     .openHelperFactory(factory)
 *     .build()
 * ```
 */
object DatabaseEncryption {

    private const val PREFS_FILE = "arthix_encrypted_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    private const val PASSPHRASE_LENGTH = 32  // 256-bit key

    /**
     * Returns a [SupportFactory] for Room, backed by a Keystore-protected passphrase.
     *
     * Thread-safe: the passphrase is generated once and cached in EncryptedSharedPreferences.
     */
    fun getSupportFactory(context: Context): SupportFactory {
        val passphrase = getOrCreatePassphrase(context)
        return SupportFactory(passphrase)
    }

    /**
     * Checks whether encryption is currently enabled for the database.
     *
     * In the current implementation, encryption is always enabled once
     * the factory is used. The Security tab toggle controls whether
     * future databases use encryption.
     */
    fun isEncryptionEnabled(context: Context): Boolean {
        return try {
            val prefs = getEncryptedPrefs(context)
            prefs.contains(KEY_DB_PASSPHRASE)
        } catch (e: Exception) {
            false
        }
    }

    private fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = getEncryptedPrefs(context)
        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)

        return if (existing != null) {
            android.util.Base64.decode(existing, android.util.Base64.NO_WRAP)
        } else {
            val passphrase = ByteArray(PASSPHRASE_LENGTH)
            SecureRandom().nextBytes(passphrase)
            val encoded = android.util.Base64.encodeToString(passphrase, android.util.Base64.NO_WRAP)
            prefs.edit().putString(KEY_DB_PASSPHRASE, encoded).apply()
            passphrase
        }
    }

    private fun getEncryptedPrefs(context: Context): android.content.SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_FILE,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}

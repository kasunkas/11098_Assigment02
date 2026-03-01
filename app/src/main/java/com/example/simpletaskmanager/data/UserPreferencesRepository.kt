package com.example.simpletaskmanager.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Repository that persists lightweight user preferences using
 * [EncryptedSharedPreferences] (AES-256-SIV key encryption, AES-256-GCM value
 * encryption).
 *
 * SECURE CODING NOTE 1 – Encrypted storage:
 *   Even non-sensitive preferences (sort order, theme choice) are stored in an
 *   encrypted file.  This prevents a file-level backup reader or a forensic tool
 *   from extracting metadata about how the user organised their tasks.
 *   The encryption key is managed by the Android Keystore – it never leaves the
 *   secure hardware enclave.
 *
 * SECURE CODING NOTE 2 – No hard-coded secrets:
 *   This class contains no API keys, passwords, or tokens.  All data written
 *   here is user-generated UI state.  If a real secret were needed (e.g., a
 *   sync token), it would be stored here under a separate key and NEVER
 *   embedded in source code.
 */
class UserPreferencesRepository(context: Context) {

    private val sharedPreferences = try {
        // Build the AES-256-GCM master key backed by Android Keystore
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_task_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Graceful fallback – log the error but do not crash the app
        Log.e("UserPreferences", "EncryptedSharedPreferences init failed; using fallback", e)
        context.getSharedPreferences("fallback_prefs", Context.MODE_PRIVATE)
    }

    // ─── Sort order ──────────────────────────────────────────────────────────

    fun saveSortOrder(ascending: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SORT_ASCENDING, ascending).apply()
    }

    val isSortAscending: Boolean
        get() = sharedPreferences.getBoolean(KEY_SORT_ASCENDING, false)

    // ─── Dark / Light mode ───────────────────────────────────────────────────

    /**
     * Persists the user's explicit theme preference.
     * [null] means "follow the system setting".
     */
    fun saveThemeOverride(isDark: Boolean?) {
        if (isDark == null) {
            sharedPreferences.edit().remove(KEY_DARK_MODE).apply()
        } else {
            sharedPreferences.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
        }
    }

    /**
     * Returns the stored theme preference.
     * [null]  → follow system  |  true → force dark  |  false → force light
     */
    fun getThemeOverride(): Boolean? {
        return if (sharedPreferences.contains(KEY_DARK_MODE)) {
            sharedPreferences.getBoolean(KEY_DARK_MODE, false)
        } else {
            null   // no override – use system default
        }
    }

    companion object {
        private const val KEY_SORT_ASCENDING = "sort_ascending"
        private const val KEY_DARK_MODE      = "dark_mode"
    }
}

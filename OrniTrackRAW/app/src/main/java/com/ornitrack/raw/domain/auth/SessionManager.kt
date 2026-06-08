package com.ornitrack.raw.domain.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * EncryptedSharedPreferences-based session manager for secure JWT token storage.
 *
 * Enforces cryptographic isolation using:
 * - MasterKey.KeyScheme.AES256_GCM
 * - PrefKeyEncryptionScheme.AES256_SIV
 * - PrefValueEncryptionScheme.AES256_GCM
 */
class SessionManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "ornitrack_secure_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_PHOTOGRAPHER_ID = "photographer_id"
        private const val KEY_PHOTOGRAPHER_EMAIL = "photographer_email"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_FULL_NAME = "full_name"

        private val TAG = SessionManager::class.simpleName
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val gson = Gson()

    // ============================================================
    // JWT Token Management
    // ============================================================

    /**
     * Save the JWT authentication token securely.
     */
    fun saveJwtToken(token: String) {
        securePrefs.edit().putString(KEY_JWT_TOKEN, token).apply()
        securePrefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
        android.util.Log.d(TAG, "JWT token saved securely")
    }

    /**
     * Retrieve the stored JWT token.
     */
    fun getJwtToken(): String? {
        return securePrefs.getString(KEY_JWT_TOKEN, null)
    }

    /**
     * Save the refresh token for token renewal.
     */
    fun saveRefreshToken(token: String) {
        securePrefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    /**
     * Retrieve the stored refresh token.
     */
    fun getRefreshToken(): String? {
        return securePrefs.getString(KEY_REFRESH_TOKEN, null)
    }

    // ============================================================
    // Session State
    // ============================================================

    /**
     * Check if the user has an active session.
     */
    fun isLoggedIn(): Boolean {
        return securePrefs.getBoolean(KEY_IS_LOGGED_IN, false) &&
                getJwtToken() != null
    }

    /**
     * Save the authenticated user's profile information.
     */
    fun saveUserSession(
        photographerId: String,
        email: String,
        fullName: String
    ) {
        securePrefs.edit().apply {
            putString(KEY_PHOTOGRAPHER_ID, photographerId)
            putString(KEY_PHOTOGRAPHER_EMAIL, email)
            putString(KEY_FULL_NAME, fullName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * Get the current photographer's ID.
     */
    fun getPhotographerId(): String? {
        return securePrefs.getString(KEY_PHOTOGRAPHER_ID, null)
    }

    /**
     * Get the current user's email.
     */
    fun getPhotographerEmail(): String? {
        return securePrefs.getString(KEY_PHOTOGRAPHER_EMAIL, null)
    }

    /**
     * Get the current user's full name.
     */
    fun getFullName(): String? {
        return securePrefs.getString(KEY_FULL_NAME, null)
    }

    // ============================================================
    // Any other secure key-value storage
    // ============================================================

    /**
     * Store an arbitrary string value securely.
     */
    fun putSecureString(key: String, value: String) {
        securePrefs.edit().putString(key, value).apply()
    }

    /**
     * Retrieve an arbitrary string value.
     */
    fun getSecureString(key: String, defaultValue: String? = null): String? {
        return securePrefs.getString(key, defaultValue)
    }

    // ============================================================
    // Clear Session (Logout)
    // ============================================================

    /**
     * Clear all stored session data (logout).
     */
    fun clearSession() {
        securePrefs.edit().clear().apply()
        android.util.Log.d(TAG, "Session cleared")
    }

    /**
     * Clear the JWT token only while keeping other preferences.
     */
    fun invalidateToken() {
        securePrefs.edit().apply {
            remove(KEY_JWT_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }
}
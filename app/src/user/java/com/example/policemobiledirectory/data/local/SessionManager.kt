package com.example.policemobiledirectory.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_prefs")

class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Keys moved down

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    val userEmail: Flow<String> = context.dataStore.data.map { it[USER_EMAIL] ?: "" }
    val isAdmin: Flow<Boolean> = context.dataStore.data.map { it[IS_ADMIN] ?: false }
    val userNotificationsSeenAt: Flow<Long> = context.dataStore.data.map { it[USER_NOTIF_LAST_SEEN] ?: 0L }
    val adminNotificationsSeenAt: Flow<Long> = context.dataStore.data.map { it[ADMIN_NOTIF_LAST_SEEN] ?: 0L }
    val launchCount: Flow<Long> = context.dataStore.data.map { it[LAUNCH_COUNT] ?: 0L }
    val successfulEventsCount: Flow<Long> = context.dataStore.data.map { it[SUCCESSFUL_EVENTS_COUNT] ?: 0L }
    val lastRatingRequestTime: Flow<Long> = context.dataStore.data.map { it[LAST_RATING_REQUEST_TIME] ?: 0L }
    val hasRatedOrNeverShow: Flow<Boolean> = context.dataStore.data.map { it[HAS_RATED_OR_NEVER_SHOW] ?: false }
    val driveAccountEmail: Flow<String?> = context.dataStore.data.map { it[DRIVE_ACCOUNT_EMAIL] }

    suspend fun saveLogin(email: String, isAdmin: Boolean) {
        context.dataStore.edit {
            it[IS_LOGGED_IN] = true
            it[USER_EMAIL] = email
            it[IS_ADMIN] = isAdmin
        }
    }

    suspend fun setUserNotificationsSeen(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[USER_NOTIF_LAST_SEEN] = timestamp
        }
    }

    suspend fun setAdminNotificationsSeen(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[ADMIN_NOTIF_LAST_SEEN] = timestamp
        }
    }

    suspend fun incrementLaunchCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[LAUNCH_COUNT] ?: 0L
            prefs[LAUNCH_COUNT] = current + 1
        }
    }

    suspend fun incrementSuccessfulEventsCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[SUCCESSFUL_EVENTS_COUNT] ?: 0L
            prefs[SUCCESSFUL_EVENTS_COUNT] = current + 1
        }
    }

    suspend fun setLastRatingRequestTime(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_RATING_REQUEST_TIME] = timestamp
        }
    }

    suspend fun setHasRatedOrNeverShow(hasRated: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_RATED_OR_NEVER_SHOW] = hasRated
        }
    }

    suspend fun saveDriveAccountEmail(email: String) {
        context.dataStore.edit { it[DRIVE_ACCOUNT_EMAIL] = email }
    }

    // --- Settings / Preferences ---
    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_ADMIN = booleanPreferencesKey("is_admin")
        val USER_NOTIF_LAST_SEEN = longPreferencesKey("user_notif_last_seen")
        val ADMIN_NOTIF_LAST_SEEN = longPreferencesKey("admin_notif_last_seen")
        val LAUNCH_COUNT = longPreferencesKey("launch_count")
        val SUCCESSFUL_EVENTS_COUNT = longPreferencesKey("successful_events_count")
        val LAST_RATING_REQUEST_TIME = longPreferencesKey("last_rating_request_time")
        val HAS_RATED_OR_NEVER_SHOW = booleanPreferencesKey("has_rated_or_never_show")
        val DRIVE_ACCOUNT_EMAIL = stringPreferencesKey("drive_account_email")
        
        // Settings Keys
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val FONT_SCALE = androidx.datastore.preferences.core.floatPreferencesKey("font_scale")

        // Biometric Keys
        val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        val ENCRYPTED_PIN = stringPreferencesKey("encrypted_pin")
        val BIOMETRIC_IV = stringPreferencesKey("biometric_iv")

        // Lockout Keys
        val FAILED_PIN_ATTEMPTS = androidx.datastore.preferences.core.intPreferencesKey("failed_pin_attempts")
        val PIN_LOCKOUT_TIMESTAMP = longPreferencesKey("pin_lockout_timestamp")

        // Session Expiry Key
        val LAST_ACTIVE_TIME = longPreferencesKey("last_active_time")

        // Search Preferences
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val STARRED_CONTACTS = stringSetPreferencesKey("starred_contacts")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[IS_DARK_THEME] ?: false }
    val fontScale: Flow<Float> = context.dataStore.data.map { it[FONT_SCALE] ?: 1.0f }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[IS_BIOMETRIC_ENABLED] ?: false }
    val encryptedPin: Flow<String?> = context.dataStore.data.map { it[ENCRYPTED_PIN] }
    val biometricIv: Flow<String?> = context.dataStore.data.map { it[BIOMETRIC_IV] }

    val failedPinAttempts: Flow<Int> = context.dataStore.data.map { it[FAILED_PIN_ATTEMPTS] ?: 0 }
    val pinLockoutTimestamp: Flow<Long> = context.dataStore.data.map { it[PIN_LOCKOUT_TIMESTAMP] ?: 0L }
    val lastActiveTime: Flow<Long> = context.dataStore.data.map { it[LAST_ACTIVE_TIME] ?: 0L }

    /** Returns the last 5 recent searches as an ordered list (most recent first). */
    val recentSearches: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[RECENT_SEARCHES] ?: ""
        if (raw.isBlank()) emptyList() else raw.split("|||").filter { it.isNotBlank() }
    }

    /** Returns the Set of starred contact IDs. */
    val starredContacts: Flow<Set<String>> = context.dataStore.data.map { it[STARRED_CONTACTS] ?: emptySet() }

    suspend fun saveBiometricCredentials(encryptedPin: String, iv: String) {
        context.dataStore.edit { prefs ->
            prefs[IS_BIOMETRIC_ENABLED] = true
            prefs[ENCRYPTED_PIN] = encryptedPin
            prefs[BIOMETRIC_IV] = iv
        }
    }

    suspend fun disableBiometrics() {
        context.dataStore.edit { prefs ->
            prefs[IS_BIOMETRIC_ENABLED] = false
            prefs.remove(ENCRYPTED_PIN)
            prefs.remove(BIOMETRIC_IV)
        }
    }

    suspend fun incrementFailedPinAttempts() {
        context.dataStore.edit { prefs ->
            val current = prefs[FAILED_PIN_ATTEMPTS] ?: 0
            val next = current + 1
            prefs[FAILED_PIN_ATTEMPTS] = next
            if (next >= 5) {
                prefs[PIN_LOCKOUT_TIMESTAMP] = System.currentTimeMillis()
            }
        }
    }

    suspend fun resetFailedPinAttempts() {
        context.dataStore.edit { prefs ->
            prefs[FAILED_PIN_ATTEMPTS] = 0
            prefs[PIN_LOCKOUT_TIMESTAMP] = 0L
        }
    }

    suspend fun updateLastActiveTime() {
        context.dataStore.edit { prefs ->
            prefs[LAST_ACTIVE_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * Prepends [query] to the recent searches list, capping at 5 entries.
     * Duplicate entries are removed (case-insensitive) before prepending.
     */
    suspend fun addRecentSearch(query: String) {
        if (query.isBlank()) return
        context.dataStore.edit { prefs ->
            val raw = prefs[RECENT_SEARCHES] ?: ""
            val current = if (raw.isBlank()) emptyList() else raw.split("|||").filter { it.isNotBlank() }
            val updated = (listOf(query.trim()) + current.filter { !it.equals(query.trim(), ignoreCase = true) }).take(5)
            prefs[RECENT_SEARCHES] = updated.joinToString("|||")
        }
    }

    /** Removes a specific [query] from the recent searches list. */
    suspend fun removeRecentSearch(query: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[RECENT_SEARCHES] ?: ""
            val current = if (raw.isBlank()) emptyList() else raw.split("|||").filter { it.isNotBlank() }
            val updated = current.filter { !it.equals(query.trim(), ignoreCase = true) }
            prefs[RECENT_SEARCHES] = updated.joinToString("|||")
        }
    }

    /**
     * Adds [contactId] to starred set if not present, or removes if already starred.
     * Returns `true` if the contact is now starred, `false` if it was unstarred.
     */
    suspend fun toggleStarredContact(contactId: String): Boolean {
        var isNowStarred = false
        context.dataStore.edit { prefs ->
            val current = prefs[STARRED_CONTACTS] ?: emptySet()
            isNowStarred = contactId !in current
            prefs[STARRED_CONTACTS] = if (isNowStarred) current + contactId else current - contactId
        }
        return isNowStarred
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_DARK_THEME] = isDark
        }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[FONT_SCALE] = scale
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            val isBiometric = prefs[IS_BIOMETRIC_ENABLED] ?: false
            val encryptedPin = prefs[ENCRYPTED_PIN]
            val iv = prefs[BIOMETRIC_IV]
            val email = prefs[USER_EMAIL]
            val isDark = prefs[IS_DARK_THEME]
            val scale = prefs[FONT_SCALE]
            val launches = prefs[LAUNCH_COUNT]
            val events = prefs[SUCCESSFUL_EVENTS_COUNT]
            val ratingTime = prefs[LAST_RATING_REQUEST_TIME]
            val rated = prefs[HAS_RATED_OR_NEVER_SHOW]
            val driveEmail = prefs[DRIVE_ACCOUNT_EMAIL]
            val failedAttempts = prefs[FAILED_PIN_ATTEMPTS]
            val lockoutTime = prefs[PIN_LOCKOUT_TIMESTAMP]

            prefs.clear()

            prefs[IS_LOGGED_IN] = false
            if (isBiometric) {
                prefs[IS_BIOMETRIC_ENABLED] = true
                if (encryptedPin != null) prefs[ENCRYPTED_PIN] = encryptedPin
                if (iv != null) prefs[BIOMETRIC_IV] = iv
                if (email != null) prefs[USER_EMAIL] = email
            }
            if (isDark != null) prefs[IS_DARK_THEME] = isDark
            if (scale != null) prefs[FONT_SCALE] = scale
            if (launches != null) prefs[LAUNCH_COUNT] = launches
            if (events != null) prefs[SUCCESSFUL_EVENTS_COUNT] = events
            if (ratingTime != null) prefs[LAST_RATING_REQUEST_TIME] = ratingTime
            if (rated != null) prefs[HAS_RATED_OR_NEVER_SHOW] = rated
            if (driveEmail != null) prefs[DRIVE_ACCOUNT_EMAIL] = driveEmail
            if (failedAttempts != null) prefs[FAILED_PIN_ATTEMPTS] = failedAttempts
            if (lockoutTime != null) prefs[PIN_LOCKOUT_TIMESTAMP] = lockoutTime
        }
    }
}

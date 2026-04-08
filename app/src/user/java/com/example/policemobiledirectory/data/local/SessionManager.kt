package com.example.policemobiledirectory.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[IS_DARK_THEME] ?: false }
    val fontScale: Flow<Float> = context.dataStore.data.map { it[FONT_SCALE] ?: 1.0f }

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
        context.dataStore.edit {
            it.clear()
        }
    }
}

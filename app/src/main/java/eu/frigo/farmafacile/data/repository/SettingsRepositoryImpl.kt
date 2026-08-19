package eu.frigo.farmafacile.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.frigo.farmafacile.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("farmafacile_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_EXPIRY_DAYS = "key_expiry_reminder_days"
        private const val KEY_SYNC_CONSENT = "key_sync_consent_granted"
    }

    override fun getExpiryReminderDays(): Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_EXPIRY_DAYS) {
                trySend(prefs.getInt(KEY_EXPIRY_DAYS, 30))
            }
        }
        trySend(prefs.getInt(KEY_EXPIRY_DAYS, 30))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setExpiryReminderDays(days: Int) {
        prefs.edit().putInt(KEY_EXPIRY_DAYS, days).apply()
    }

    override fun isSyncConsentGranted(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SYNC_CONSENT) {
                trySend(prefs.getBoolean(KEY_SYNC_CONSENT, false))
            }
        }
        trySend(prefs.getBoolean(KEY_SYNC_CONSENT, false))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setSyncConsentGranted(granted: Boolean) {
        prefs.edit().putBoolean(KEY_SYNC_CONSENT, granted).apply()
    }
}

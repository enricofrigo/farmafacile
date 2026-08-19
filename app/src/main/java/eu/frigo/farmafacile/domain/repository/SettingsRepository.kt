package eu.frigo.farmafacile.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getExpiryReminderDays(): Flow<Int>
    suspend fun setExpiryReminderDays(days: Int)
    fun isSyncConsentGranted(): Flow<Boolean>
    suspend fun setSyncConsentGranted(granted: Boolean)
}

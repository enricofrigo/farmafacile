package eu.frigo.farmafacile.data.remote.drive

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        const val DRIVE_API_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        const val MIME_TYPE_JSON = "application/json"
        const val MIME_TYPE_FOLDER = "application/vnd.google-apps.folder"
        const val ROOT_FOLDER_NAME = "MedTrack_Lists"
    }

    /**
     * Serializes a list payload to a JSON string or local backup file.
     */
    fun serializePayload(payload: DriveListSyncPayload): String {
        return gson.toJson(payload)
    }

    /**
     * Deserializes a JSON string into a [DriveListSyncPayload].
     */
    fun deserializePayload(json: String): DriveListSyncPayload? {
        return runCatching { gson.fromJson(json, DriveListSyncPayload::class.java) }.getOrNull()
    }

    /**
     * Saves payload locally as an exportable/shareable JSON backup file in app files directory.
     */
    suspend fun saveLocalBackupFile(listId: String, payload: DriveListSyncPayload): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "drive_exports").apply { mkdirs() }
        val file = File(dir, "list_${listId}.json")
        file.writeText(serializePayload(payload))
        file
    }

    /**
     * Reads a local JSON file payload.
     */
    suspend fun readLocalBackupFile(file: File): DriveListSyncPayload? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        deserializePayload(file.readText())
    }
}

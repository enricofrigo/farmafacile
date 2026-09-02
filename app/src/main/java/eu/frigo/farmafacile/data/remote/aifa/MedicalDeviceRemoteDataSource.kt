package eu.frigo.farmafacile.data.remote.aifa

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.frigo.farmafacile.data.remote.ResilientFileDownloader
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicalDeviceRemoteDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloader: ResilientFileDownloader
) {
    companion object {
        const val SALUTE_DISPOSITIVI_MEDICI_ZIP_URL = "https://www.dati.salute.gov.it/sites/default/files/opendata/DISPO_RDM_1_20260824_csv.zip"
        const val TEMP_FILE_NAME = "dispositivi_medici.zip"
    }

    /**
     * Downloads the official Medical Devices ZIP dataset with auto-resume to a temporary cache file.
     */
    suspend fun downloadMedicalDevicesZipFile(
        onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)? = null
    ): File {
        val tempFile = File(context.cacheDir, TEMP_FILE_NAME)
        return downloader.downloadFileWithResume(
            url = SALUTE_DISPOSITIVI_MEDICI_ZIP_URL,
            destinationFile = tempFile,
            onProgress = onProgress
        )
    }
}

package eu.frigo.farmafacile.data.remote.aifa

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AifaRemoteDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        const val AIFA_CONFEZIONI_FORNITURA_URL = "https://drive.aifa.gov.it/farmaci/confezioni_fornitura.csv"
        const val AIFA_CONFEZIONI_FALLBACK_URL = "https://drive.aifa.gov.it/farmaci/confezioni.csv"
    }

    /**
     * Streams the official AIFA CSV dataset.
     */
    fun downloadAifaCsvStream(): InputStream {
        val request = Request.Builder()
            .url(AIFA_CONFEZIONI_FORNITURA_URL)
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful || response.body == null) {
            // Fallback attempt
            val fallbackRequest = Request.Builder()
                .url(AIFA_CONFEZIONI_FALLBACK_URL)
                .get()
                .build()
            val fallbackResponse = okHttpClient.newCall(fallbackRequest).execute()
            if (!fallbackResponse.isSuccessful || fallbackResponse.body == null) {
                throw IllegalStateException("Impossibile scaricare il catalogo farmaci AIFA (HTTP ${response.code})")
            }
            return fallbackResponse.body!!.byteStream()
        }

        return response.body!!.byteStream()
    }
}

package com.groundwork.programmieramt.fi

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class JournalResponse(
    val date: String,
    val structuredFacts: List<String>,
    val storedCount: Int,
    val failedCount: Int
)

@Singleton
class JournalClient @Inject constructor(
    private val configStore: JournalConfigStore
) {
    private fun buildClient(config: WebDavConfig): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
        if (config.trustAllCerts) {
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
            }
            builder.sslSocketFactory(sslContext.socketFactory, trustAll)
            builder.hostnameVerifier { _, _ -> true }
        }
        return builder.build()
    }

    private fun execute(config: WebDavConfig, requestBuilder: Request.Builder): okhttp3.Response {
        val client = buildClient(config)
        val response = client.newCall(requestBuilder.build()).execute()
        if (response.code == 401 && config.username.isNotBlank()) {
            response.close()
            requestBuilder.header("Authorization", Credentials.basic(config.username, config.password))
            return client.newCall(requestBuilder.build()).execute()
        }
        return response
    }

    fun transcribe(audioBytes: ByteArray, filename: String = "recording.m4a"): Result<String> {
        val config = configStore.get()
            ?: return Result.failure(Exception("Journal nicht konfiguriert"))
        return try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "audio", filename,
                    audioBytes.toRequestBody("audio/mp4".toMediaType())
                )
                .build()
            val request = Request.Builder()
                .url(config.url.trimEnd('/') + "/transcribe")
                .post(body)
            val response = execute(config, request)
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }
            val text = JSONObject(responseBody).getString("text")
            Result.success(text)
        } catch (e: Exception) {
            Timber.e(e, "Journal transcribe failed")
            Result.failure(e)
        }
    }

    fun saveJournal(text: String, date: String): Result<JournalResponse> {
        val config = configStore.get()
            ?: return Result.failure(Exception("Journal nicht konfiguriert"))
        return try {
            val json = JSONObject().apply {
                put("text", text)
                put("date", date)
            }
            val request = Request.Builder()
                .url(config.url.trimEnd('/') + "/journal")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
            val response = execute(config, request)
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }
            val obj = JSONObject(responseBody)
            val factsArray = obj.getJSONArray("structured_facts")
            val facts = (0 until factsArray.length()).map { factsArray.getString(it) }
            Result.success(
                JournalResponse(
                    date = obj.getString("date"),
                    structuredFacts = facts,
                    storedCount = obj.getInt("stored_count"),
                    failedCount = obj.getInt("failed_count")
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Journal save failed")
            Result.failure(e)
        }
    }

    fun search(query: String): Result<List<String>> {
        val config = configStore.get()
            ?: return Result.failure(Exception("Journal nicht konfiguriert"))
        return try {
            val request = Request.Builder()
                .url(config.url.trimEnd('/') + "/entries?query=" + java.net.URLEncoder.encode(query, "UTF-8"))
                .get()
            val response = execute(config, request)
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }
            val arr = JSONObject(responseBody).getJSONArray("entries")
            val entries = (0 until arr.length()).map { arr.getString(it) }
            Result.success(entries)
        } catch (e: Exception) {
            Timber.e(e, "Journal search failed")
            Result.failure(e)
        }
    }

    fun testConnection(): Result<Unit> {
        val config = configStore.get()
            ?: return Result.failure(Exception("Journal nicht konfiguriert"))
        return try {
            val request = Request.Builder()
                .url(config.url.trimEnd('/') + "/health")
                .get()
            val response = execute(config, request)
            response.close()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("HTTP ${response.code}"))
        } catch (e: Exception) {
            Timber.e(e, "Journal connection test failed")
            Result.failure(e)
        }
    }
}

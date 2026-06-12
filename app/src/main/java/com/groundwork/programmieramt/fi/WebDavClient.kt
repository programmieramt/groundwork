package com.groundwork.programmieramt.fi

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.StringReader
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Singleton
class WebDavClient @Inject constructor(
    private val configStore: WebDavConfigStore
) {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun buildClient(config: WebDavConfig): OkHttpClient {
        val builder = OkHttpClient.Builder()
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

    private fun credential(config: WebDavConfig) = Credentials.basic(config.username, config.password)

    fun testConnection(): Result<Unit> {
        val config = configStore.get() ?: return Result.failure(Exception("Keine WebDAV-Konfiguration"))
        return try {
            val client = buildClient(config)
            val req = Request.Builder()
                .url(config.url.trimEnd('/') + "/")
                .method("PROPFIND", "".toRequestBody())
                .header("Authorization", credential(config))
                .header("Depth", "0")
                .build()
            val resp = client.newCall(req).execute()
            resp.close()
            if (resp.isSuccessful || resp.code == 207) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.code}"))
        } catch (e: Exception) {
            Timber.e(e, "WebDAV test connection failed")
            Result.failure(e)
        }
    }

    fun ensureDirectory(path: String, config: WebDavConfig? = null): Result<Unit> {
        val cfg = config ?: configStore.get() ?: return Result.failure(Exception("Keine WebDAV-Konfiguration"))
        return try {
            val client = buildClient(cfg)
            val url = cfg.url.trimEnd('/') + "/" + path.trimStart('/')
            val req = Request.Builder()
                .url(url)
                .method("MKCOL", "".toRequestBody())
                .header("Authorization", credential(cfg))
                .build()
            val resp = client.newCall(req).execute()
            resp.close()
            // 201 = created, 405 = already exists — both are fine
            if (resp.isSuccessful || resp.code == 405) Result.success(Unit)
            else Result.failure(Exception("MKCOL HTTP ${resp.code}"))
        } catch (e: Exception) {
            Timber.e(e, "WebDAV MKCOL failed: $path")
            Result.failure(e)
        }
    }

    fun put(path: String, json: String): Result<Unit> {
        val config = configStore.get() ?: return Result.failure(Exception("Keine WebDAV-Konfiguration"))
        return try {
            val client = buildClient(config)
            val url = config.url.trimEnd('/') + "/" + path.trimStart('/')
            val req = Request.Builder()
                .url(url)
                .put(json.toRequestBody(jsonMedia))
                .header("Authorization", credential(config))
                .build()
            val resp = client.newCall(req).execute()
            resp.close()
            if (resp.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("PUT HTTP ${resp.code}"))
        } catch (e: Exception) {
            Timber.e(e, "WebDAV PUT failed: $path")
            Result.failure(e)
        }
    }

    fun putBytes(path: String, bytes: ByteArray, mediaType: String, config: WebDavConfig? = null): Result<Unit> {
        val cfg = config ?: configStore.get() ?: return Result.failure(Exception("Keine WebDAV-Konfiguration"))
        return try {
            val client = buildClient(cfg)
            val url = cfg.url.trimEnd('/') + "/" + path.trimStart('/')
            val req = Request.Builder()
                .url(url)
                .put(bytes.toRequestBody(mediaType.toMediaType()))
                .header("Authorization", credential(cfg))
                .build()
            val resp = client.newCall(req).execute()
            resp.close()
            if (resp.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("PUT HTTP ${resp.code}"))
        } catch (e: Exception) {
            Timber.e(e, "WebDAV PUT failed: $path")
            Result.failure(e)
        }
    }

    fun delete(path: String): Result<Unit> {
        val config = configStore.get() ?: return Result.failure(Exception("Keine WebDAV-Konfiguration"))
        return try {
            val client = buildClient(config)
            val url = config.url.trimEnd('/') + "/" + path.trimStart('/')
            val req = Request.Builder()
                .url(url)
                .delete()
                .header("Authorization", credential(config))
                .build()
            val resp = client.newCall(req).execute()
            resp.close()
            // 404 = already gone — treat as success
            if (resp.isSuccessful || resp.code == 404) Result.success(Unit)
            else Result.failure(Exception("DELETE HTTP ${resp.code}"))
        } catch (e: Exception) {
            Timber.e(e, "WebDAV DELETE failed: $path")
            Result.failure(e)
        }
    }

    fun get(path: String): Result<String> {
        val config = configStore.get() ?: return Result.failure(Exception("Keine WebDAV-Konfiguration"))
        return try {
            val client = buildClient(config)
            val url = config.url.trimEnd('/') + "/" + path.trimStart('/')
            val req = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", credential(config))
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                Result.success(resp.body?.string() ?: "")
            } else {
                resp.close()
                Result.failure(Exception("GET HTTP ${resp.code}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "WebDAV GET failed: $path")
            Result.failure(e)
        }
    }

    fun listFiles(path: String): Result<List<String>> {
        val config = configStore.get() ?: return Result.failure(Exception("Keine WebDAV-Konfiguration"))
        return try {
            val client = buildClient(config)
            val url = config.url.trimEnd('/') + "/" + path.trimStart('/')
            val req = Request.Builder()
                .url(url)
                .method("PROPFIND", "".toRequestBody())
                .header("Authorization", credential(config))
                .header("Depth", "1")
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: ""
            resp.close()
            if (!resp.isSuccessful && resp.code != 207) {
                return Result.failure(Exception("PROPFIND HTTP ${resp.code}"))
            }
            Result.success(parsePropfindHrefs(body, url))
        } catch (e: Exception) {
            Timber.e(e, "WebDAV PROPFIND failed: $path")
            Result.failure(e)
        }
    }

    private fun parsePropfindHrefs(xml: String, baseUrl: String): List<String> {
        val hrefs = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            var inHref = false
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> inHref = parser.name.endsWith("href")
                    XmlPullParser.TEXT -> if (inHref) {
                        val href = parser.text.trim()
                        // skip the collection itself
                        if (!href.endsWith("/") && href != baseUrl && href.isNotBlank()) {
                            hrefs.add(href)
                        }
                        inHref = false
                    }
                    XmlPullParser.END_TAG -> inHref = false
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            Timber.e(e, "PROPFIND XML parse error")
        }
        return hrefs
    }
}

package com.groundwork.programmieramt.fi

import android.graphics.Canvas
import com.groundwork.programmieramt.pen.PdfRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UltrabridgeExporter @Inject constructor(
    private val client: WebDavClient,
    private val configStore: UltrabridgeConfigStore
) {
    private val ensuredDirs = mutableSetOf<String>()

    suspend fun export(folder: String, id: Long, strokesJson: String, drawTemplate: (Canvas, Int, Int) -> Unit) =
        withContext(Dispatchers.IO) {
            val config = configStore.get() ?: return@withContext
            try {
                ensureDir("groundwork-export", config)
                ensureDir("groundwork-export/$folder", config)

                val pdfBytes = PdfRenderer.render(strokesJson, drawTemplate)
                client.putBytes("groundwork-export/$folder/$id.pdf", pdfBytes, "application/pdf", config)
                    .onFailure { Timber.e(it, "Ultrabridge export failed: $folder/$id") }
            } catch (e: Exception) {
                Timber.e(e, "Ultrabridge export failed: $folder/$id")
            }
        }

    private fun ensureDir(path: String, config: WebDavConfig) {
        if (ensuredDirs.add(path)) {
            client.ensureDirectory(path, config)
        }
    }
}

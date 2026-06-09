package com.groundwork.programmieramt.fi

import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.io.OutputStream
import java.util.*
import javax.inject.Inject

class GoogleDriveService @Inject constructor() {
    companion object {

        fun getFile(drive: Drive, parent: File, fileName: String): File? {
            val files = drive.files().list().setSpaces("appDataFolder")
                .setQ("'${parent.id}' in parents and name='$fileName' and trashed=false")
                .setFields("nextPageToken, files(id, kind, name, size, mimeType, createdTime, modifiedTime, properties, parents)")
                .execute().files.sortedByDescending { it.createdTime.value }
            return if (files.isNotEmpty()) files[0] else null
        }

        fun getOrCreateRootFolder(drive: Drive, root: String): File? {
            val files = drive.files().list().setSpaces("appDataFolder")
                .setQ("mimeType='application/vnd.google-apps.folder' and name='$root' and trashed=false")
                .setFields("nextPageToken, files(id, kind, name, size, mimeType, createdTime, modifiedTime, properties, parents)")
                .execute().files.sortedByDescending { it.createdTime.value }
            if (files.isNotEmpty()) return files[0]

            val folder = File()
                .setParents(Collections.singletonList("appDataFolder"))
                .setMimeType("application/vnd.google-apps.folder")
                .setName(root)
            return drive.files().create(folder).execute()
        }

        fun getOrCreateFolder(drive: Drive, parent: File, folderName: String): File? {
            val files = drive.files().list().setSpaces("appDataFolder")
                .setQ("mimeType='application/vnd.google-apps.folder' and name='$folderName' and '${parent.id}' in parents and trashed=false")
                .setFields("nextPageToken, files(id, kind, name, size, mimeType, createdTime, modifiedTime, properties, parents)")
                .execute().files.sortedByDescending { it.createdTime.value }
            if (files.isNotEmpty()) return files[0]

            val folder = File()
                .setParents(Collections.singletonList(parent.id))
                .setMimeType("application/vnd.google-apps.folder")
                .setName(folderName)
            return drive.files().create(folder).execute()
        }

        fun getOrCreatePath(drive: Drive, parent: File, path: String): File? {
            var current = parent
            path.split("/").filter { it.isNotEmpty() }.forEach {
                current = getOrCreateFolder(drive, current, it) ?: return null
            }
            return current
        }

        fun uploadFile(drive: Drive, parent: File, fileName: String, content: AbstractInputStreamContent, properties: Map<String, String>?): File? {
            val exists = getFile(drive, parent, fileName)
            val fileMetadata = File().setParents(null).setProperties(properties).setName(fileName)
            if (exists == null) {
                fileMetadata.setParents(Collections.singletonList(parent.id))
                return drive.files().create(fileMetadata, content).execute()
            }
            return drive.files().update(exists.id, fileMetadata, content).execute()
        }

        fun downloadFile(drive: Drive, file: File, outputStream: OutputStream) {
            drive.files().get(file.id).executeMediaAndDownloadTo(outputStream)
        }

        fun listFiles(drive: Drive, parent: File): List<File> {
            val result = mutableListOf<File>()
            var nextPageToken: String? = null
            do {
                val response = drive.files().list().setSpaces("appDataFolder")
                    .setPageToken(nextPageToken)
                    .setQ("'${parent.id}' in parents and trashed=false")
                    .setFields("nextPageToken, files(id, kind, name, size, mimeType, createdTime, modifiedTime, properties, parents)")
                    .execute()
                result.addAll(response.files)
                nextPageToken = response.nextPageToken
            } while (nextPageToken != null)
            return result.sortedBy { it.name }
        }
    }
}

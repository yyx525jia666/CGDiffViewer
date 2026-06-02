package com.example.imageviewer.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtils {
    fun zipFolder(context: Context, folderUri: Uri, zipUri: Uri): Boolean {
        return try {
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
            context.contentResolver.openOutputStream(zipUri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                    folder.listFiles().forEach { file ->
                        if (file.isFile) {
                            context.contentResolver.openInputStream(file.uri)?.use { input ->
                                val entry = ZipEntry(file.name)
                                zipOut.putNextEntry(entry)
                                input.copyTo(zipOut)
                                zipOut.closeEntry()
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

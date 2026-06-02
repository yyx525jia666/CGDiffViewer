package com.example.imageviewer.ui.merge

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imageviewer.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MergeViewModel(application: Application) : AndroidViewModel(application) {

    private val _projectFolder = MutableStateFlow<Uri?>(null)
    val projectFolder: StateFlow<Uri?> = _projectFolder.asStateFlow()

    private val _sourceFolders = MutableStateFlow<List<SourceFolder>>(emptyList())
    val sourceFolders: StateFlow<List<SourceFolder>> = _sourceFolders.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    data class SourceFolder(
        val uri: Uri,
        val name: String,
        val imageCount: Int
    )

    fun setProjectFolder(uri: Uri) {
        _projectFolder.value = uri
    }

    fun addSourceFolder(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            var count = 0
            val name = DocumentFile.fromTreeUri(context, uri)?.name ?: "未知文件夹"
            
            withContext(Dispatchers.IO) {
                val rootDoc = DocumentFile.fromTreeUri(context, uri)
                rootDoc?.listFiles()?.forEach { file ->
                    if (file.isFile && FileUtils.isImageFile(file.name ?: "")) {
                        count++
                    }
                }
            }
            
            val newList = _sourceFolders.value.toMutableList()
            newList.add(SourceFolder(uri, name, count))
            _sourceFolders.value = newList
        }
    }

    fun removeSourceFolder(index: Int) {
        val newList = _sourceFolders.value.toMutableList()
        if (index in newList.indices) {
            newList.removeAt(index)
            _sourceFolders.value = newList
        }
    }

    fun moveFolder(from: Int, to: Int) {
        val newList = _sourceFolders.value.toMutableList()
        if (from in newList.indices && to in newList.indices) {
            val item = newList.removeAt(from)
            newList.add(to, item)
            _sourceFolders.value = newList
        }
    }

    fun startMerge() {
        val targetUri = _projectFolder.value ?: return
        val sources = _sourceFolders.value
        if (sources.isEmpty()) return

        viewModelScope.launch {
            _isProcessing.value = true
            val context = getApplication<Application>()
            
            withContext(Dispatchers.IO) {
                val targetDoc = DocumentFile.fromTreeUri(context, targetUri) ?: return@withContext
                var globalIndex = 1
                
                for (source in sources) {
                    val sourceDoc = DocumentFile.fromTreeUri(context, source.uri) ?: continue
                    val files = sourceDoc.listFiles()
                        .filter { it.isFile && FileUtils.isImageFile(it.name ?: "") }
                        .sortedBy { it.name }
                    
                    for (file in files) {
                        val extension = file.name?.substringAfterLast(".", "jpg") ?: "jpg"
                        val newName = "${globalIndex++}.$extension"
                        
                        // 复制文件到目标文件夹
                        // 注意：DocumentFile 没有直接的 copyTo。需要通过流复制。
                        val newFile = targetDoc.createFile(file.type ?: "image/jpeg", newName)
                        if (newFile != null) {
                            context.contentResolver.openInputStream(file.uri)?.use { input ->
                                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            }
            _isProcessing.value = false
        }
    }
}

package com.example.imageviewer.ui.deletion

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
import java.io.File

class DeletionViewModel(application: Application) : AndroidViewModel(application) {

    private val _images = MutableStateFlow<List<ImageItem>>(emptyList())
    val images: StateFlow<List<ImageItem>> = _images.asStateFlow()

    private val _selectedIndices = MutableStateFlow<Set<Int>>(emptySet())
    val selectedIndices: StateFlow<Set<Int>> = _selectedIndices.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var currentFolderUri: Uri? = null

    data class ImageItem(
        val uri: Uri,
        val name: String,
        val path: String
    )

    fun loadFolder(uri: Uri) {
        currentFolderUri = uri
        viewModelScope.launch {
            _isProcessing.value = true
            val context = getApplication<Application>()
            val imageItems = mutableListOf<ImageItem>()
            
            withContext(Dispatchers.IO) {
                val rootDoc = DocumentFile.fromTreeUri(context, uri)
                rootDoc?.listFiles()?.forEach { file ->
                    if (file.isFile && FileUtils.isImageFile(file.name ?: "")) {
                        imageItems.add(
                            ImageItem(
                                uri = file.uri,
                                name = file.name ?: "未知",
                                path = file.uri.toString()
                            )
                        )
                    }
                }
            }
            
            // 排序
            val sortedList = imageItems.sortedBy { it.name }
            _images.value = sortedList
            _selectedIndices.value = emptySet()
            _isProcessing.value = false
        }
    }

    fun toggleSelection(index: Int) {
        val currentSet = _selectedIndices.value.toMutableSet()
        if (currentSet.contains(index)) {
            currentSet.remove(index)
        } else {
            currentSet.add(index)
        }
        _selectedIndices.value = currentSet
    }

    fun selectRange(start: Int, end: Int) {
        val currentSet = _selectedIndices.value.toMutableSet()
        val range = if (start <= end) start..end else end..start
        for (i in range) {
            currentSet.add(i)
        }
        _selectedIndices.value = currentSet
    }

    fun confirmDeletion() {
        val indicesToDelete = _selectedIndices.value.sortedDescending()
        if (indicesToDelete.isEmpty()) return

        viewModelScope.launch {
            _isProcessing.value = true
            val context = getApplication<Application>()
            
            withContext(Dispatchers.IO) {
                val currentList = _images.value.toMutableList()
                val deletedItems = mutableListOf<ImageItem>()
                
                // 1. 删除选中的文件
                for (index in indicesToDelete) {
                    val item = currentList[index]
                    val file = DocumentFile.fromSingleUri(context, item.uri)
                    if (file?.delete() == true) {
                        deletedItems.add(item)
                        currentList.removeAt(index)
                    }
                }

                // 2. 顺延重命名
                // 为了简单起见，我们重新对剩下的文件按 1, 2, 3... 命名
                // 获取文件扩展名，假设都是同一种或者保留原扩展名
                for (i in currentList.indices) {
                    val item = currentList[i]
                    val extension = item.name.substringAfterLast(".", "jpg")
                    val newName = "${i + 1}.$extension"
                    
                    if (item.name != newName) {
                        val docFile = DocumentFile.fromSingleUri(context, item.uri)
                        docFile?.renameTo(newName)
                    }
                }
            }
            
            // 重新加载文件夹以更新状态
            currentFolderUri?.let { loadFolder(it) }
        }
    }
}

package com.example.imageviewer.ui.deletion

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imageviewer.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _processingStage = MutableStateFlow("")
    val processingStage: StateFlow<String> = _processingStage.asStateFlow()

    private val _event = MutableSharedFlow<String>()
    val event = _event.asSharedFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private var currentFolderUri: Uri? = null
    private var lastDeletedItems = mutableListOf<List<DeletedSession>>()

    data class ImageItem(
        val uri: Uri,
        val name: String,
        val path: String
    )

    data class DeletedSession(
        val originalUri: Uri,
        val originalName: String,
        val tempFileUri: Uri
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
            
            // 排序 (自然排序)
            val sortedList = imageItems.sortedWith(compareBy<ImageItem> { it.name.length }.thenBy { it.name })
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

    fun exitFolder() {
        currentFolderUri = null
        _images.value = emptyList()
        _selectedIndices.value = emptySet()
        _canUndo.value = lastDeletedItems.isNotEmpty()
    }

    fun confirmDeletion() {
        val currentList = _images.value
        val indicesToDelete = _selectedIndices.value.sortedDescending()
        if (indicesToDelete.isEmpty()) return

        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0f
            _processingStage.value = "正在备份文件..."
            val context = getApplication<Application>()
            
            try {
                withContext(Dispatchers.IO) {
                    val session = mutableListOf<DeletedSession>()
                    
                    // 1. 获取根目录用于创建临时文件夹
                    val rootUri = currentFolderUri ?: return@withContext
                    val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext
                    val deletedDir = rootDoc.findFile(".deleted") ?: rootDoc.createDirectory(".deleted")
                    
                    val total = indicesToDelete.size
                    // 2. 将选中的文件移动到 .deleted 文件夹 (作为备份支持撤回)
                    for ((indexInLoop, index) in indicesToDelete.withIndex()) {
                        val item = currentList.getOrNull(index) ?: continue
                        val file = DocumentFile.fromSingleUri(context, item.uri)
                        
                        if (file != null && deletedDir != null) {
                            // 由于 DocumentFile 没有 move，我们先复制再删除
                            val tempName = "undo_${System.currentTimeMillis()}_${item.name}"
                            val tempFile = deletedDir.createFile(file.type ?: "image/jpeg", tempName)
                            if (tempFile != null) {
                                context.contentResolver.openInputStream(file.uri)?.use { input ->
                                    context.contentResolver.openOutputStream(tempFile.uri)?.use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                session.add(DeletedSession(item.uri, item.name, tempFile.uri))
                                file.delete()
                            }
                        }
                        _progress.value = (indexInLoop + 1).toFloat() / total / 2 // 占据前半段进度
                    }

                    if (session.isNotEmpty()) {
                        lastDeletedItems.add(0, session)
                        _canUndo.value = true
                    }
                }
                
                // 3. 重新加载并重命名
                currentFolderUri?.let { uri ->
                    val context = getApplication<Application>()
                    val imageItems = mutableListOf<ImageItem>()
                    
                    _processingStage.value = "正在进行重命名，加载较慢..."
                    withContext(Dispatchers.IO) {
                        val rootDoc = DocumentFile.fromTreeUri(context, uri)
                        rootDoc?.listFiles()?.forEach { file ->
                            if (file.isFile && FileUtils.isImageFile(file.name ?: "") && file.name?.startsWith("undo_") != true) {
                                imageItems.add(
                                    ImageItem(
                                        uri = file.uri,
                                        name = file.name ?: "未知",
                                        path = file.uri.toString()
                                    )
                                )
                            }
                        }
                        
                        // 排序 (自然排序)
                        val sortedList = imageItems.sortedWith(compareBy<ImageItem> { it.name.length }.thenBy { it.name })
                        
                        // 重命名
                        val totalRename = sortedList.size
                        for (i in sortedList.indices) {
                            val item = sortedList[i]
                            val extension = item.name.substringAfterLast(".", "jpg")
                            val newName = "${i + 1}.$extension"
                            
                            if (item.name != newName) {
                                val docFile = rootDoc?.findFile(item.name) ?: DocumentFile.fromSingleUri(context, item.uri)
                                docFile?.renameTo(newName)
                            }
                            _progress.value = 0.5f + (i + 1).toFloat() / totalRename.coerceAtLeast(1) / 2 // 占据后半段进度
                        }
                    }
                }
                _event.emit("删除成功")
            } catch (e: Exception) {
                e.printStackTrace()
                _event.emit("操作失败: ${e.localizedMessage ?: e.javaClass.simpleName}")
            } finally {
                // 重新加载文件夹以更新状态
                currentFolderUri?.let { loadFolder(it) }
                _isProcessing.value = false
            }
        }
    }

    fun undo() {
        if (lastDeletedItems.isEmpty()) return
        
        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0f
            _processingStage.value = "正在撤回操作..."
            val context = getApplication<Application>()
            val session = lastDeletedItems.removeAt(0)
            
            try {
                withContext(Dispatchers.IO) {
                    val total = session.size
                    for ((index, deletedItem) in session.withIndex()) {
                        val tempFile = DocumentFile.fromSingleUri(context, deletedItem.tempFileUri)
                        val parentUri = currentFolderUri ?: continue
                        val parentDoc = DocumentFile.fromTreeUri(context, parentUri) ?: continue
                        
                        if (tempFile != null && tempFile.exists()) {
                            val restoredFile = parentDoc.createFile(tempFile.type ?: "image/jpeg", deletedItem.originalName)
                            if (restoredFile != null) {
                                context.contentResolver.openInputStream(tempFile.uri)?.use { input ->
                                    context.contentResolver.openOutputStream(restoredFile.uri)?.use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                tempFile.delete()
                            }
                        }
                        _progress.value = (index + 1).toFloat() / total
                    }
                }
                _event.emit("撤回成功")
            } catch (e: Exception) {
                _event.emit("撤回失败: ${e.message}")
            } finally {
                _canUndo.value = lastDeletedItems.isNotEmpty()
                currentFolderUri?.let { loadFolder(it) }
                _isProcessing.value = false
            }
        }
    }
}

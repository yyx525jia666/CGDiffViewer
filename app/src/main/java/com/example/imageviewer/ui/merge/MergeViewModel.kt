package com.example.imageviewer.ui.merge

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imageviewer.data.AppDatabase
import com.example.imageviewer.data.MergeProject
import com.example.imageviewer.util.FileUtils
import com.example.imageviewer.util.ZipUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MergeViewModel(application: Application) : AndroidViewModel(application) {

    private val mergeDao = AppDatabase.getDatabase(application).mergeDao()
    private val gson = Gson()

    val allProjects: StateFlow<List<MergeProject>> = mergeDao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentProject = MutableStateFlow<MergeProject?>(null)
    val currentProject: StateFlow<MergeProject?> = _currentProject.asStateFlow()

    private val _sourceFolders = MutableStateFlow<List<SourceFolder>>(emptyList())
    val sourceFolders: StateFlow<List<SourceFolder>> = _sourceFolders.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _event = MutableSharedFlow<String>()
    val event = _event.asSharedFlow()

    private val _folderImages = MutableStateFlow<List<Uri>>(emptyList())
    val folderImages: StateFlow<List<Uri>> = _folderImages.asStateFlow()

    data class SourceFolder(
        val uri: Uri,
        val name: String,
        val imageCount: Int,
        val thumbnailUri: Uri?
    )

    fun createProject(name: String, targetUri: Uri) {
        viewModelScope.launch {
            val project = MergeProject(
                name = name,
                targetFolderUri = targetUri.toString(),
                sourceFoldersJson = "[]"
            )
            val id = mergeDao.insertProject(project)
            _currentProject.value = project.copy(id = id)
            _sourceFolders.value = emptyList()
        }
    }

    fun selectProject(project: MergeProject) {
        _currentProject.value = project
        val type = object : TypeToken<List<SourceFolder>>(){}.type
        val folders: List<SourceFolder> = try {
            val list = gson.fromJson<List<SourceFolderSaved>>(project.sourceFoldersJson, object : TypeToken<List<SourceFolderSaved>>(){}.type)
            list.map { it.toSourceFolder() }
        } catch (e: Exception) {
            emptyList()
        }
        _sourceFolders.value = folders
    }

    fun closeProject() {
        _currentProject.value = null
        _sourceFolders.value = emptyList()
    }

    private data class SourceFolderSaved(val uri: String, val name: String, val imageCount: Int, val thumbnailUri: String?) {
        fun toSourceFolder() = SourceFolder(Uri.parse(uri), name, imageCount, thumbnailUri?.let { Uri.parse(it) })
    }
    
    private fun SourceFolder.toSaved() = SourceFolderSaved(uri.toString(), name, imageCount, thumbnailUri?.toString())

    private fun saveCurrentFolders() {
        val project = _currentProject.value ?: return
        val json = gson.toJson(_sourceFolders.value.map { it.toSaved() })
        viewModelScope.launch {
            val updatedProject = project.copy(sourceFoldersJson = json)
            mergeDao.updateProject(updatedProject)
            _currentProject.value = updatedProject
        }
    }

    fun addSourceFolder(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            var count = 0
            var firstImageUri: Uri? = null
            val name = DocumentFile.fromTreeUri(context, uri)?.name ?: "未知文件夹"
            
            withContext(Dispatchers.IO) {
                val rootDoc = DocumentFile.fromTreeUri(context, uri)
                rootDoc?.listFiles()?.forEach { file ->
                    if (file.isFile && FileUtils.isImageFile(file.name ?: "")) {
                        if (firstImageUri == null) firstImageUri = file.uri
                        count++
                    }
                }
            }
            
            val newList = _sourceFolders.value.toMutableList()
            newList.add(SourceFolder(uri, name, count, firstImageUri))
            _sourceFolders.value = newList
            saveCurrentFolders()
        }
    }

    fun removeSourceFolder(index: Int) {
        val newList = _sourceFolders.value.toMutableList()
        if (index in newList.indices) {
            newList.removeAt(index)
            _sourceFolders.value = newList
            saveCurrentFolders()
        }
    }

    fun moveFolder(from: Int, to: Int) {
        val newList = _sourceFolders.value.toMutableList()
        if (from in newList.indices && to in newList.indices) {
            val item = newList.removeAt(from)
            newList.add(to, item)
            _sourceFolders.value = newList
            saveCurrentFolders()
        }
    }

    fun deleteProject(project: MergeProject) {
        viewModelScope.launch {
            mergeDao.deleteProject(project)
            if (_currentProject.value?.id == project.id) {
                closeProject()
            }
        }
    }

    fun selectFolderForView(uri: Uri) {
        _folderImages.value = emptyList() // 立即清除旧数据，防止闪现
        viewModelScope.launch {
            _folderImages.value = getImagesInFolder(uri)
        }
    }

    suspend fun getImagesInFolder(uri: Uri): List<Uri> {
        val context = getApplication<Application>()
        val images = mutableListOf<DocumentFile>()
        withContext(Dispatchers.IO) {
            val rootDoc = DocumentFile.fromTreeUri(context, uri)
            rootDoc?.listFiles()?.forEach { file ->
                if (file.isFile && FileUtils.isImageFile(file.name ?: "")) {
                    images.add(file)
                }
            }
        }
        // 使用自然排序 (长度 -> 名称)
        return images.sortedWith(compareBy<DocumentFile> { it.name?.length ?: 0 }.thenBy { it.name ?: "" })
            .map { it.uri }
    }

    fun startMerge() {
        val project = _currentProject.value ?: return
        val targetUri = Uri.parse(project.targetFolderUri)
        val sources = _sourceFolders.value
        if (sources.isEmpty()) return

        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0f
            val context = getApplication<Application>()
            
            try {
                withContext(Dispatchers.IO) {
                    val targetDoc = DocumentFile.fromTreeUri(context, targetUri) ?: return@withContext
                    
                    // 1. 清理目标文件夹中已存在的图片文件
                    targetDoc.listFiles().forEach { file ->
                        if (file.isFile && FileUtils.isImageFile(file.name ?: "")) {
                            file.delete()
                        }
                    }

                    val totalImages = sources.sumOf { it.imageCount }
                    var globalIndex = 1
                    
                    for (source in sources) {
                        val sourceDoc = DocumentFile.fromTreeUri(context, source.uri) ?: continue
                        val files = sourceDoc.listFiles()
                            .filter { it.isFile && FileUtils.isImageFile(it.name ?: "") }
                            .sortedWith(compareBy<DocumentFile> { it.name?.length ?: 0 }.thenBy { it.name ?: "" })
                        
                        for (file in files) {
                            val extension = file.name?.substringAfterLast(".", "jpg") ?: "jpg"
                            val newName = "${globalIndex++}.$extension"
                            
                            val newFile = targetDoc.createFile(file.type ?: "image/jpeg", newName)
                            if (newFile != null) {
                                context.contentResolver.openInputStream(file.uri)?.use { input ->
                                    context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            _progress.value = (globalIndex - 1).toFloat() / totalImages.coerceAtLeast(1)
                        }
                    }
                }
                _event.emit("合并成功")
            } catch (e: Exception) {
                _event.emit("合并失败: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportToZip() {
        val project = _currentProject.value ?: return
        val projectUri = Uri.parse(project.targetFolderUri)
        viewModelScope.launch {
            _isProcessing.value = true
            val context = getApplication<Application>()
            
            withContext(Dispatchers.IO) {
                val projectDoc = DocumentFile.fromTreeUri(context, projectUri) ?: return@withContext
                val zipName = "${projectDoc.name ?: "project"}.zip"
                
                val zipFile = projectDoc.createFile("application/zip", zipName)
                if (zipFile != null) {
                    ZipUtils.zipFolder(context, projectUri, zipFile.uri)
                }
            }
            _isProcessing.value = false
        }
    }
}

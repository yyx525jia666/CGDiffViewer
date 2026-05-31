package com.example.imageviewer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imageviewer.data.AppDatabase
import com.example.imageviewer.data.ImageSequence
import com.example.imageviewer.data.SettingsDataStore
import com.example.imageviewer.util.FileUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val historyDao = database.historyDao()
    private val settingsDataStore = SettingsDataStore(application)

    private val _sequences = MutableStateFlow<List<ImageSequence>>(emptyList())
    val sequences: StateFlow<List<ImageSequence>> = _sequences.asStateFlow()

    private val _defaultSpeed = MutableStateFlow(1.0)
    val defaultSpeed: StateFlow<Double> = _defaultSpeed.asStateFlow()

    private val _longPressMultiplier = MutableStateFlow(2.0)
    val longPressMultiplier: StateFlow<Double> = _longPressMultiplier.asStateFlow()

    private val _autoHideTime = MutableStateFlow(3)
    val autoHideTime: StateFlow<Int> = _autoHideTime.asStateFlow()

    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    init {
        loadSequences()
        loadSettings()
    }

    private fun loadSequences() {
        viewModelScope.launch {
            historyDao.getRecentSequences().collect { list ->
                _sequences.value = list
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _defaultSpeed.value = settingsDataStore.defaultSpeed.first()
            _longPressMultiplier.value = settingsDataStore.longPressMultiplier.first()
            _autoHideTime.value = settingsDataStore.autoHideTime.first()
            _themeMode.value = settingsDataStore.themeMode.first()
        }
    }

    fun addSequenceFromFolder(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val contentResolver = context.contentResolver
            val imagePaths = mutableListOf<String>()

            val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
                uri,
                android.provider.DocumentsContract.getTreeDocumentId(uri)
            )

            val cursor = contentResolver.query(
                childrenUri,
                arrayOf(
                    android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null, null, null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val docId = it.getString(0)
                    val name = it.getString(1)

                    if (FileUtils.isImageFile(name)) {
                        val fileUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                            uri, docId
                        )
                        imagePaths.add(fileUri.toString())
                    }
                }
            }

            if (imagePaths.isNotEmpty()) {
                val sortedPaths = FileUtils.sortFileNames(imagePaths)
                val folderName = uri.lastPathSegment?.substringAfterLast(":")?.substringAfterLast("/")
                    ?: "未知文件夹"
                val coverPath = sortedPaths.first()

                val sequence = ImageSequence(
                    name = folderName.ifEmpty { "未知文件夹" },
                    coverPath = coverPath,
                    imagePaths = Gson().toJson(sortedPaths)
                )

                historyDao.insertSequence(sequence)
            }
        }
    }

    fun addSequenceFromFiles(uris: List<Uri>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val imagePaths = mutableListOf<String>()

            for (uri in uris) {
                val name = FileUtils.getFileNameFromUri(context, uri)
                if (FileUtils.isImageFile(name)) {
                    imagePaths.add(uri.toString())
                }
            }

            if (imagePaths.isNotEmpty()) {
                val sortedPaths = FileUtils.sortFileNames(imagePaths)
                val name = "序列 ${System.currentTimeMillis() % 10000}"
                val coverPath = sortedPaths.first()

                val sequence = ImageSequence(
                    name = name,
                    coverPath = coverPath,
                    imagePaths = Gson().toJson(sortedPaths)
                )

                historyDao.insertSequence(sequence)
            }
        }
    }

    fun deleteSequence(sequence: ImageSequence) {
        viewModelScope.launch {
            historyDao.deleteSequence(sequence)
        }
    }

    fun updateDefaultSpeed(speed: Double) {
        viewModelScope.launch {
            settingsDataStore.setDefaultSpeed(speed)
            _defaultSpeed.value = speed
        }
    }

    fun updateLongPressMultiplier(multiplier: Double) {
        viewModelScope.launch {
            settingsDataStore.setLongPressMultiplier(multiplier)
            _longPressMultiplier.value = multiplier
        }
    }

    fun updateAutoHideTime(seconds: Int) {
        viewModelScope.launch {
            settingsDataStore.setAutoHideTime(seconds)
            _autoHideTime.value = seconds
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
            _themeMode.value = mode
        }
    }
}

package com.example.imageviewer

import android.app.Activity
import android.view.WindowInsets
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.imageviewer.data.AppDatabase
import com.example.imageviewer.data.ImageSequence
import com.example.imageviewer.data.SettingsDataStore
import com.example.imageviewer.ui.components.PlayerControls
import com.example.imageviewer.util.SpeedUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    sequenceId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val database = remember { AppDatabase.getDatabase(context) }
    val historyDao = remember { database.historyDao() }
    val settingsDataStore = remember { SettingsDataStore(context) }

    var sequence by remember { mutableStateOf<ImageSequence?>(null) }
    var imagePaths by remember { mutableStateOf<List<String>>(emptyList()) }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(1.0) }
    var autoHideTime by remember { mutableIntStateOf(3) }

    var lastPainter by remember { mutableStateOf<Painter?>(null) }

    var showControls by remember { mutableStateOf(true) }
    var isUserInteracting by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    // 处理系统返回手势
    BackHandler {
        scope.launch {
            sequence?.let { seq ->
                historyDao.updateLastPlayedIndex(seq.id, currentIndex)
            }
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        speed = settingsDataStore.defaultSpeed.first()
        autoHideTime = settingsDataStore.autoHideTime.first()
    }

    LaunchedEffect(sequenceId) {
        sequence = historyDao.getSequenceById(sequenceId)
        sequence?.let { seq ->
            val type = object : TypeToken<List<String>>() {}.type
            imagePaths = Gson().fromJson(seq.imagePaths, type)
            currentIndex = seq.lastPlayedIndex.coerceIn(0, (imagePaths.size - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(showControls, isUserInteracting, isPlaying) {
        if (showControls && !isUserInteracting && isPlaying) {
            delay(autoHideTime * 1000L)
            showControls = false
        }
    }

    LaunchedEffect(isPlaying, speed) {
        if (isPlaying && imagePaths.isNotEmpty()) {
            while (true) {
                val delayMillis = SpeedUtils.toDelayMillis(speed)
                delay(delayMillis)

                if (currentIndex < imagePaths.size - 1) {
                    currentIndex++
                } else {
                    isPlaying = false
                    // 播放结束时也保存一下
                    sequence?.let { seq ->
                        historyDao.updateLastPlayedIndex(seq.id, currentIndex)
                    }
                    break
                }
            }
        } else {
            // 当停止播放时，保存进度
            sequence?.let { seq ->
                historyDao.updateLastPlayedIndex(seq.id, currentIndex)
            }
        }
    }

    // 预加载下一批图片
    LaunchedEffect(currentIndex, imagePaths, isPlaying) {
        if (isPlaying && imagePaths.isNotEmpty()) {
            val preloadCount = 10
            val loader = context.imageLoader
            for (i in 1..preloadCount) {
                val nextIndex = currentIndex + i
                if (nextIndex < imagePaths.size) {
                    val request = ImageRequest.Builder(context)
                        .data(imagePaths[nextIndex])
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build()
                    loader.enqueue(request)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sequence?.let { seq ->
                scope.launch {
                    historyDao.updateLastPlayedIndex(seq.id, currentIndex)
                }
            }
        }
    }

    DisposableEffect(isFullscreen) {
        val window = (context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)

        if (isFullscreen) {
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsets.Type.systemBars())
        }

        onDispose {
            controller.show(WindowInsets.Type.systemBars())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                    },
                    onDoubleTap = {
                        isPlaying = !isPlaying
                    }
                )
            }
    ) {
        if (imagePaths.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imagePaths[currentIndex])
                    .crossfade(false)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "图片 ${currentIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                placeholder = lastPainter,
                onSuccess = { state ->
                    lastPainter = state.painter
                }
            )

            /* 移除了顶部的返回按钮，改为由 PlayerControls 提供 */

            PlayerControls(
                visible = showControls,
                isPlaying = isPlaying,
                currentIndex = currentIndex,
                totalImages = imagePaths.size,
                speed = speed,
                isFullscreen = isFullscreen,
                onPlayPauseClick = { isPlaying = !isPlaying },
                onSeek = { index ->
                    currentIndex = index
                    isUserInteracting = false
                },
                onSpeedChange = { newSpeed ->
                    speed = SpeedUtils.clampSpeed(newSpeed)
                    isUserInteracting = false
                },
                onFullscreenToggle = { isFullscreen = !isFullscreen },
                onBack = {
                    // 点击返回按钮时保存进度
                    scope.launch {
                        sequence?.let { seq ->
                            historyDao.updateLastPlayedIndex(seq.id, currentIndex)
                        }
                        onBack()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // 重新添加左上角的返回按钮
            if (showControls) {
                IconButton(
                    onClick = {
                        scope.launch {
                            sequence?.let { seq ->
                                historyDao.updateLastPlayedIndex(seq.id, currentIndex)
                            }
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
            }

            /* 移除了长按倍速提示窗口 */
        } else {
            Text(
                text = "加载中...",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {}
    }
}

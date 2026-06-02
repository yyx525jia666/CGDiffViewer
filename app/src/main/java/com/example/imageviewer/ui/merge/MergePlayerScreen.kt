package com.example.imageviewer.ui.merge

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.imageviewer.ui.theme.ProgressColor

@Composable
fun MergePlayerScreen(
    // 此处需要逻辑处理单张删除，目前先传入图片列表
    imageUris: List<android.net.Uri>,
    onBack: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var currentList by remember { mutableStateOf(imageUris) }
    var deletedStack = remember { mutableStateListOf<Pair<Int, android.net.Uri>>() }
    var lastPainter by remember { mutableStateOf<androidx.compose.ui.graphics.painter.Painter?>(null) }

    // 关键修复：当传入的图片列表发生变化时，同步更新内部状态
    LaunchedEffect(imageUris) {
        if (imageUris.isNotEmpty()) {
            currentList = imageUris
            currentIndex = 0
            deletedStack.clear()
        }
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (currentList.isNotEmpty()) {
            val currentUri = currentList[currentIndex.coerceIn(0, currentList.size - 1)]
            
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentUri)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                placeholder = lastPainter,
                onSuccess = { state ->
                    lastPainter = state.painter
                }
            )

            // 点击区域
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxHeight().weight(1f).clickable(remember { MutableInteractionSource() }, null) {
                    if (currentIndex > 0) currentIndex--
                })
                Box(modifier = Modifier.fillMaxHeight().weight(1f).clickable(remember { MutableInteractionSource() }, null) {
                    if (currentIndex < currentList.size - 1) currentIndex++
                })
            }

            // 控制层
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onBack, modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), MaterialTheme.shapes.extraLarge)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }

                    Row {
                        IconButton(
                            onClick = {
                                if (deletedStack.isNotEmpty()) {
                                    val (idx, uri) = deletedStack.removeAt(deletedStack.size - 1)
                                    val newList = currentList.toMutableList()
                                    newList.add(idx, uri)
                                    currentList = newList
                                    if (idx <= currentIndex) currentIndex++
                                }
                            },
                            enabled = deletedStack.isNotEmpty(),
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), MaterialTheme.shapes.extraLarge)
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "撤回", tint = Color.White)
                        }
                        
                        IconButton(
                            onClick = {
                                val uri = currentList[currentIndex]
                                deletedStack.add(currentIndex to uri)
                                val newList = currentList.toMutableList()
                                newList.removeAt(currentIndex)
                                currentList = newList
                                if (currentIndex >= currentList.size) currentIndex = (currentList.size - 1).coerceAtLeast(0)
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), MaterialTheme.shapes.extraLarge)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 底部进度条
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.medium).padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Slider(
                        value = currentIndex.toFloat(),
                        onValueChange = { currentIndex = it.toInt() },
                        valueRange = 0f..(currentList.size - 1).coerceAtLeast(1).toFloat(),
                        colors = SliderDefaults.colors(thumbColor = ProgressColor, activeTrackColor = ProgressColor)
                    )
                    Text("${currentIndex + 1} / ${currentList.size}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

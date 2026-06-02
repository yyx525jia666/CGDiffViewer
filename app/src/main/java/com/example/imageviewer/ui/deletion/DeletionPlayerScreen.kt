package com.example.imageviewer.ui.deletion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
fun DeletionPlayerScreen(
    viewModel: DeletionViewModel,
    initialIndex: Int,
    onBack: () -> Unit
) {
    val images by viewModel.images.collectAsState()
    val selectedIndices by viewModel.selectedIndices.collectAsState()
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, images.size - 1)) }
    var lastPainter by remember { mutableStateOf<androidx.compose.ui.graphics.painter.Painter?>(null) }
    
    val currentItem = if (images.isNotEmpty()) images[currentIndex] else null
    val isSelected = selectedIndices.contains(currentIndex)

    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (currentItem != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentItem.uri)
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

            // 点击区域：左侧上一张，右侧下一张
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentIndex > 0) currentIndex--
                        }
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentIndex < images.size - 1) currentIndex++
                        }
                )
            }

            // 控制层
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), MaterialTheme.shapes.extraLarge)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }

                    // 选中/取消选中 按钮
                    IconButton(
                        onClick = { viewModel.toggleSelection(currentIndex) },
                        modifier = Modifier.background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else Color.Black.copy(alpha = 0.3f),
                            MaterialTheme.shapes.extraLarge
                        )
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "选择",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 底部进度条
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Slider(
                        value = currentIndex.toFloat(),
                        onValueChange = { currentIndex = it.toInt() },
                        valueRange = 0f..(images.size - 1).coerceAtLeast(1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = ProgressColor,
                            activeTrackColor = ProgressColor
                        )
                    )
                    Text(
                        text = "${currentIndex + 1} / ${images.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

package com.example.imageviewer.ui.deletion

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletionScreen(
    viewModel: DeletionViewModel,
    onOpenFullScreen: (Int) -> Unit
) {
    val images by viewModel.images.collectAsState()
    val selectedIndices by viewModel.selectedIndices.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val processingStage by viewModel.processingStage.collectAsState()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        viewModel.event.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.loadFolder(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图片删除") },
                navigationIcon = {
                    if (images.isNotEmpty()) {
                        IconButton(onClick = { viewModel.exitFolder() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "退出文件夹")
                        }
                    }
                },
                actions = {
                    val canUndo by viewModel.canUndo.collectAsState()
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = canUndo
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "撤回")
                    }
                }
            )
        },
        floatingActionButton = {
            if (images.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.confirmDeletion() },
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "确认删除")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (images.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = { folderLauncher.launch(null) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("选择文件夹")
                    }
                }
            } else {
                var initialIndex by remember { mutableIntStateOf(-1) }
                
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(images) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val index = findIndexAtOffset(offset, gridState, 3)
                                    if (index != -1) {
                                        initialIndex = index
                                        viewModel.toggleSelection(index)
                                    }
                                },
                                onDrag = { change, _ ->
                                    if (initialIndex != -1) {
                                        val currentIndex = findIndexAtOffset(change.position, gridState, 3)
                                        if (currentIndex != -1) {
                                            viewModel.selectRange(initialIndex, currentIndex)
                                        }
                                    }
                                },
                                onDragEnd = { initialIndex = -1 },
                                onDragCancel = { initialIndex = -1 }
                            )
                        }
                ) {
                    itemsIndexed(images) { index, item ->
                        val isSelected = selectedIndices.contains(index)
                        ImageItemView(
                            item = item,
                            isSelected = isSelected,
                            onToggleSelection = { viewModel.toggleSelection(index) },
                            onDoubleClick = { onOpenFullScreen(index) }
                        )
                    }
                }
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = processingStage,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageItemView(
    item: DeletionViewModel.ImageItem,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onDoubleClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleSelection() },
                    onDoubleTap = { onDoubleClick() }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 左上角选中状态图标
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.2f), MaterialTheme.shapes.small)
        )
    }
}

private fun findIndexAtOffset(
    offset: androidx.compose.ui.geometry.Offset,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    columns: Int
): Int {
    val layoutInfo = gridState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return -1

    val matchedItem = visibleItems.find { item ->
        val itemBounds = androidx.compose.ui.geometry.Rect(
            item.offset.x.toFloat(),
            item.offset.y.toFloat(),
            (item.offset.x + item.size.width).toFloat(),
            (item.offset.y + item.size.height).toFloat()
        )
        itemBounds.contains(offset)
    }
    
    return matchedItem?.index ?: -1
}

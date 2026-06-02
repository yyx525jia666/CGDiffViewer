package com.example.imageviewer.ui.merge

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.imageviewer.data.MergeProject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeScreen(
    viewModel: MergeViewModel,
    onViewFolder: (Uri) -> Unit
) {
    val currentProject by viewModel.currentProject.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val sourceFolders by viewModel.sourceFolders.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.event.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var selectedTargetUri by remember { mutableStateOf<Uri?>(null) }

    val targetFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            selectedTargetUri = it
            if (newProjectName.isEmpty()) {
                newProjectName = it.lastPathSegment?.substringAfterLast(":") ?: "新项目"
            }
        }
    }

    val sourceFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.addSourceFolder(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentProject?.name ?: "文件夹合并") },
                navigationIcon = {
                    if (currentProject != null) {
                        IconButton(onClick = { viewModel.closeProject() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentProject == null) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "创建项目")
                }
            } else {
                FloatingActionButton(onClick = { sourceFolderLauncher.launch(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "添加文件夹")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (currentProject == null) {
                // 项目列表
                if (allProjects.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无合并项目，点击右下角创建", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allProjects) { project ->
                            ProjectItem(
                                project = project,
                                onClick = { viewModel.selectProject(project) },
                                onDelete = { viewModel.deleteProject(project) }
                            )
                        }
                    }
                }
            } else {
                // 项目详情 (合并界面)
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("待合并文件夹", style = MaterialTheme.typography.titleMedium)
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(sourceFolders) { index, folder ->
                            SourceFolderItem(
                                folder = folder,
                                onClick = { onViewFolder(folder.uri) },
                                onRemove = { viewModel.removeSourceFolder(index) },
                                onMoveUp = if (index > 0) { { viewModel.moveFolder(index, index - 1) } } else null,
                                onMoveDown = if (index < sourceFolders.size - 1) { { viewModel.moveFolder(index, index + 1) } } else null
                            )
                        }
                    }

                    if (isProcessing) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startMerge() },
                            modifier = Modifier.weight(1f),
                            enabled = sourceFolders.isNotEmpty() && !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Text("开始合并")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.exportToZip() },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing
                        ) {
                            Icon(Icons.Default.Compress, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("导出ZIP")
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("创建合并项目") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("项目名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { targetFolderLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(selectedTargetUri?.lastPathSegment?.substringAfterLast(":") ?: "选择目标文件夹")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = selectedTargetUri
                        if (newProjectName.isNotEmpty() && uri != null) {
                            viewModel.createProject(newProjectName, uri)
                            showCreateDialog = false
                            newProjectName = ""
                            selectedTargetUri = null
                        }
                    },
                    enabled = newProjectName.isNotEmpty() && selectedTargetUri != null
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ProjectItem(
    project: MergeProject,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.FolderZip,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun SourceFolderItem(
    folder: MergeViewModel.SourceFolder,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(folder.thumbnailUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(folder.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text("${folder.imageCount} 张图片", style = MaterialTheme.typography.bodySmall)
            }

            Column {
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "上移")
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "下移")
                    }
                }
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "移除")
            }
        }
    }
}

package com.example.imageviewer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.imageviewer.ui.deletion.DeletionPlayerScreen
import com.example.imageviewer.ui.deletion.DeletionScreen
import com.example.imageviewer.ui.deletion.DeletionViewModel
import com.example.imageviewer.ui.merge.MergePlayerScreen
import com.example.imageviewer.ui.merge.MergeScreen
import com.example.imageviewer.ui.merge.MergeViewModel
import com.example.imageviewer.ui.theme.ImageSequencePlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 请求所有文件访问权限 (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }

        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            
            ImageSequencePlayerTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ImageSequencePlayerApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun ImageSequencePlayerApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 创建或获取板块 ViewModel
    val deletionViewModel: DeletionViewModel = viewModel()
    val mergeViewModel: MergeViewModel = viewModel()

    val mainTabs = listOf(
        TabItem("main", "CG观看", Icons.Default.Visibility),
        TabItem("deletion", "图片删除", Icons.Default.DeleteSweep),
        TabItem("merge", "文件夹合并", Icons.Default.FolderZip)
    )

    val showBottomBar = mainTabs.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    mainTabs.forEach { tab ->
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("main") {
                MainScreen(
                    viewModel = viewModel,
                    onSequenceClick = { sequenceId ->
                        navController.navigate("player/$sequenceId")
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    }
                )
            }
            
            composable("deletion") {
                DeletionScreen(
                    viewModel = deletionViewModel,
                    onOpenFullScreen = { index ->
                        navController.navigate("deletion_player/$index")
                    }
                )
            }

            composable(
                "deletion_player/{index}",
                arguments = listOf(navArgument("index") { type = NavType.IntType })
            ) { backStackEntry ->
                val index = backStackEntry.arguments?.getInt("index") ?: 0
                DeletionPlayerScreen(
                    viewModel = deletionViewModel,
                    initialIndex = index,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("merge") {
                MergeScreen(
                    viewModel = mergeViewModel,
                    onViewFolder = { uri ->
                        mergeViewModel.selectFolderForView(uri)
                        navController.navigate("merge_player")
                    }
                )
            }

            composable("merge_player") {
                val images by mergeViewModel.folderImages.collectAsState()
                MergePlayerScreen(
                    imageUris = images,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                "player/{sequenceId}",
                arguments = listOf(navArgument("sequenceId") { type = NavType.LongType })
            ) { backStackEntry ->
                val sequenceId = backStackEntry.arguments?.getLong("sequenceId") ?: 0L
                PlayerScreen(
                    sequenceId = sequenceId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

data class TabItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

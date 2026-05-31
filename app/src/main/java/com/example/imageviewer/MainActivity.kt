package com.example.imageviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.imageviewer.ui.theme.ImageSequencePlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    
    NavHost(
        navController = navController,
        startDestination = "main"
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
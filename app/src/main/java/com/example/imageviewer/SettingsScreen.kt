package com.example.imageviewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.imageviewer.ui.theme.ProgressColor
import com.example.imageviewer.util.SpeedUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val defaultSpeed by viewModel.defaultSpeed.collectAsState()
    val longPressMultiplier by viewModel.longPressMultiplier.collectAsState()
    val autoHideTime by viewModel.autoHideTime.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 默认播放速度
            SettingItem(
                title = "默认播放速度 (张/秒)",
                valueRange = 0.1f..60f,
                currentValue = defaultSpeed.toFloat(),
                onValueChange = { viewModel.updateDefaultSpeed(it.toDouble()) }
            )
            
            // 长按加速倍数
            SettingItem(
                title = "长按加速倍数 (倍)",
                valueRange = 1.0f..10.0f,
                currentValue = longPressMultiplier.toFloat(),
                onValueChange = { viewModel.updateLongPressMultiplier(it.toDouble()) }
            )
            
            // 自动隐藏时间
            SettingItem(
                title = "控制面板自动隐藏时间 (秒)",
                valueRange = 1f..10f,
                currentValue = autoHideTime.toFloat(),
                onValueChange = { viewModel.updateAutoHideTime(it.toInt()) }
            )

            // 外观设置
            Text(
                text = "外观风格",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            val currentTheme by viewModel.themeMode.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeButton(
                    text = "系统默认",
                    selected = currentTheme == "system",
                    onClick = { viewModel.updateThemeMode("system") },
                    modifier = Modifier.weight(1f)
                )
                ThemeButton(
                    text = "清新白色",
                    selected = currentTheme == "white",
                    onClick = { viewModel.updateThemeMode("white") },
                    modifier = Modifier.weight(1f)
                )
                ThemeButton(
                    text = "深邃蓝色",
                    selected = currentTheme == "blue",
                    onClick = { viewModel.updateThemeMode("blue") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 速度示例
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "速度示例",
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = "• 1.0 张/秒 = 每秒显示1张图片\n" +
                       "• 10.0 张/秒 = 每秒显示10张图片\n" +
                       "• 24.0 张/秒 = 接近电影帧率\n" +
                       "• 60.0 张/秒 = 流畅动画效果",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    valueRange: ClosedFloatingPointRange<Float>,
    currentValue: Float,
    onValueChange: (Float) -> Unit
) {
    var textValue by remember(currentValue) { 
        mutableStateOf(if (currentValue % 1f == 0f) currentValue.toInt().toString() else "%.1f".format(currentValue)) 
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
                    newValue.toFloatOrNull()?.let { 
                        val clamped = it.coerceIn(valueRange)
                        onValueChange(clamped)
                    }
                },
                modifier = Modifier.width(100.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        }
        
        Slider(
            value = currentValue,
            onValueChange = {
                onValueChange(it)
            },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ProgressColor,
                activeTrackColor = ProgressColor
            ),
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "%.1f".format(valueRange.start),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = "%.1f".format(valueRange.endInclusive),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ThemeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

package com.example.imageviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.imageviewer.util.SpeedUtils

@Composable
fun SpeedControl(
    speed: Double,
    onSpeedChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var textValue by remember(showEditDialog, speed) { 
        mutableStateOf(if (speed % 1.0 == 0.0) speed.toInt().toString() else "%.1f".format(speed)) 
    }

    Box(modifier = modifier) {
        Text(
            text = "%.1f张/秒".format(speed),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .clickable { showEditDialog = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("修改播放速度") },
                text = {
                    Column {
                        Text("请输入速度 (0.1 - 60.0 张/秒)", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            textValue.toDoubleOrNull()?.let {
                                onSpeedChange(SpeedUtils.clampSpeed(it))
                            }
                            showEditDialog = false
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

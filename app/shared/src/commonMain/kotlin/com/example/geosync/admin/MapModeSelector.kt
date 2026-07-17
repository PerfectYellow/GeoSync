package com.example.geosync.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.geosync.localization.LocalStrings

@Composable
fun MapModeSelector(
    currentMode: MapMode,
    isOffline: Boolean,
    onModeSelected: (MapMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val strings = LocalStrings.current

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Map, contentDescription = "Change Map Mode")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val modes = listOf(
                MapMode.OPEN_STREET to strings.mapOpenStreet,
                MapMode.MAP_IR to strings.mapMapIr,
                MapMode.INTERNAL to strings.mapInternal,
                MapMode.OFFLINE to strings.mapOffline
            )

            modes.forEach { (mode, label) ->
                val isSelected = mode == currentMode
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    },
                    modifier = if (isSelected) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)) else Modifier
                )
            }
        }
    }
}

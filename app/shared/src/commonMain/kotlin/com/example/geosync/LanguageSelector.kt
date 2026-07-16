package com.example.geosync

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.geosync.localization.Language
import com.example.geosync.localization.LocalizationManager

@Composable
fun LanguageSelector() {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Language, contentDescription = "Change Language")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Language.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(language.flag)
                            Spacer(Modifier.width(8.dp))
                            Text(language.label)
                        }
                    },
                    onClick = {
                        LocalizationManager.currentLanguage = language
                        expanded = false
                    }
                )
            }
        }
    }
}

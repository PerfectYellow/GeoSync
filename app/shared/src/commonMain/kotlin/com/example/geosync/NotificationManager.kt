package com.example.geosync

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.geosync.localization.LocalStrings
import com.example.geosync.localization.LocalizationManager
import kotlinx.coroutines.delay

enum class NotificationType {
    SUCCESS, ERROR, INFO
}

data class NotificationData(
    val message: String,
    val type: NotificationType = NotificationType.INFO,
    val durationMillis: Long = 3000,
    val isPersistent: Boolean = false
)

object NotificationManager {
    private val _notification = mutableStateOf<NotificationData?>(null)
    val notification: State<NotificationData?> = _notification

    private val OFFLINE_MSG get() = LocalizationManager.strings.youAreOffline

    fun show(message: String, type: NotificationType = NotificationType.INFO) {
        // Prevent spamming the same message (especially connection errors)
        if (_notification.value?.message == message) return
        
        // Don't overwrite persistent (e.g., offline) notifications with transient ones
        if (_notification.value?.isPersistent == true) return
        
        _notification.value = NotificationData(message, type)
    }

    fun showOffline() {
        _notification.value = NotificationData(
            message = OFFLINE_MSG,
            type = NotificationType.ERROR,
            isPersistent = true
        )
    }

    fun dismissOffline() {
        if (_notification.value?.message == OFFLINE_MSG) {
            dismiss()
        }
    }

    fun dismiss() {
        _notification.value = null
    }
}

@Composable
fun NotificationBanner() {
    val notificationData by NotificationManager.notification
    val strings = LocalStrings.current
    
    LaunchedEffect(notificationData) {
        if (notificationData != null && !notificationData!!.isPersistent) {
            delay(notificationData!!.durationMillis)
            NotificationManager.dismiss()
        }
    }

    AnimatedVisibility(
        visible = notificationData != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        notificationData?.let { data ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (data.type) {
                        NotificationType.SUCCESS -> Color(0xFF2E7D32)
                        NotificationType.ERROR -> MaterialTheme.colorScheme.error
                        NotificationType.INFO -> MaterialTheme.colorScheme.secondary
                    },
                    contentColor = Color.White,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                            .wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (data.type) {
                                NotificationType.SUCCESS -> Icons.Default.CheckCircle
                                NotificationType.ERROR -> Icons.Default.Warning
                                NotificationType.INFO -> Icons.Default.Info
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = data.message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { NotificationManager.dismiss() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.dismiss,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

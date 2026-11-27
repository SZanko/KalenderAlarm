package at.zanko.kalenderwecker.android

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Warns when Android would silently stop the alarm from ringing on time or over the lock screen. */
@Composable
fun PermissionBanner() {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    @Suppress("UNUSED_EXPRESSION") tick
    val exact = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    val nm = context.getSystemService(NotificationManager::class.java)
    val notifications = nm.areNotificationsEnabled()
    val fullScreen = Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent()
    val problem = when {
        !notifications -> "Notifications are off – the alarm can't ring." to Settings.ACTION_APP_NOTIFICATION_SETTINGS
        !exact -> "Exact alarms are not allowed – alarms may ring late." to Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
        !fullScreen -> "Full-screen alarms are not allowed – no ring screen on the lock screen." to Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
        else -> null
    } ?: return

    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(problem.first, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = {
                context.startActivity(
                    Intent(problem.second)
                        .setData(Uri.parse("package:${context.packageName}"))
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }) { Text("Fix in settings") }
        }
    }
}

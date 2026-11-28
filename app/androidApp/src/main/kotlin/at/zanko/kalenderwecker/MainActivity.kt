package at.zanko.kalenderwecker

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import at.zanko.kalenderwecker.android.PermissionBanner
import at.zanko.kalenderwecker.android.syncSystemAppLanguage
import at.zanko.kalenderwecker.data.SettingsRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

class MainActivity : ComponentActivity() {
    private val settings: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        lifecycleScope.launch {
            // drop(1): react to in-app changes only, keep a language set in system settings.
            settings.settings.map { it.language }.distinctUntilChanged().drop(1).collect {
                syncSystemAppLanguage(this@MainActivity, it)
            }
        }

        setContent {
            val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
            LaunchedEffect(Unit) { notifications.launch(Manifest.permission.POST_NOTIFICATIONS) }
            App(banner = { PermissionBanner() })
        }
    }
}

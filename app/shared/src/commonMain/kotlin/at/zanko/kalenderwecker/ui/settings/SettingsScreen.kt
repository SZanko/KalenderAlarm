package at.zanko.kalenderwecker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.zanko.kalenderwecker.ui.formatOffset
import at.zanko.kalenderwecker.ui.formatTime
import at.zanko.kalenderwecker.ui.persistableSoundUri
import at.zanko.kalenderwecker.ui.short
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

private val SOUND_EXTENSIONS = listOf("mp3", "wav", "ogg", "m4a", "aac", "flac")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    banner: @Composable () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val urlError by viewModel.urlError.collectAsStateWithLifecycle()
    var url by rememberSaveable(settings.icsUrl) { mutableStateOf(settings.icsUrl) }
    var editing by remember { mutableStateOf<TimeField?>(null) }
    val soundPicker = rememberFilePickerLauncher(type = FileKitType.File(SOUND_EXTENSIONS)) { file ->
        if (file != null) viewModel.setSound(file.persistableSoundUri())
    }

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            banner()
            Section("TimeEdit calendar") {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Calendar link (.ics or .html)") },
                    isError = urlError,
                    supportingText = {
                        Text(
                            if (urlError) "That doesn't look like a TimeEdit calendar link"
                            else "TimeEdit → Subscribe → copy the link"
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.setUrl(url) }),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.setUrl(url) }) {
                            Icon(Icons.Rounded.Check, contentDescription = "Save link")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("Wake-up before class") {
                Text(
                    formatOffset(settings.offsetMinutes) + " before the first morning class",
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = settings.offsetMinutes.toFloat(),
                    onValueChange = { viewModel.setOffsetMinutes((it / 5).roundToInt() * 5) },
                    valueRange = 0f..180f,
                    steps = 180 / 5 - 1,
                )
                TimeRow("Morning ends at", settings.morningEnd) { editing = TimeField.MorningEnd }
            }

            Section("Fallback alarm") {
                Text(
                    "Rings on days without a morning class",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TimeRow("Time", settings.fallbackTime) { editing = TimeField.Fallback }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        val selected = day in settings.fallbackDays
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.toggleFallbackDay(day) },
                            label = { Text(day.short()) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null,
                        )
                    }
                }
            }

            Section("Wake-up sound") {
                ListItem(
                    leadingContent = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                    trailingContent = {
                        if (settings.soundUri != null) {
                            IconButton(onClick = { viewModel.setSound(null) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Use default sound")
                            }
                        }
                    },
                ) {
                    Text(settings.soundUri?.substringAfterLast('/')?.substringAfterLast("%2F") ?: "Default alarm sound")
                }
                OutlinedButton(onClick = { soundPicker.launch() }) { Text("Choose audio file…") }
            }
        }
    }

    editing?.let { field ->
        val initial = if (field == TimeField.Fallback) settings.fallbackTime else settings.morningEnd
        val pickerState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (field == TimeField.Fallback) "Fallback alarm" else "Morning ends at") },
            confirmButton = {
                TextButton(onClick = {
                    val time = LocalTime(pickerState.hour, pickerState.minute)
                    if (field == TimeField.Fallback) viewModel.setFallbackTime(time) else viewModel.setMorningEnd(time)
                    editing = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        ) {
            TimePicker(pickerState)
        }
    }
}

private enum class TimeField { MorningEnd, Fallback }

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun TimeRow(label: String, time: LocalTime, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        ListItem(
            trailingContent = {
                TextButton(onClick = onClick) {
                    Text(formatTime(time), style = MaterialTheme.typography.titleLarge)
                }
            },
        ) { Text(label) }
    }
}

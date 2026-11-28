package at.zanko.kalenderwecker.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.zanko.kalenderwecker.alarm.AlarmSource
import at.zanko.kalenderwecker.alarm.DayAlarm
import at.zanko.kalenderwecker.model.Appointment
import at.zanko.kalenderwecker.ui.displayTitle
import at.zanko.kalenderwecker.resources.Res
import at.zanko.kalenderwecker.resources.empty_action
import at.zanko.kalenderwecker.resources.empty_body
import at.zanko.kalenderwecker.resources.empty_title
import at.zanko.kalenderwecker.resources.home_subtitle
import at.zanko.kalenderwecker.resources.next_alarm
import at.zanko.kalenderwecker.resources.none_planned
import at.zanko.kalenderwecker.resources.settings
import at.zanko.kalenderwecker.resources.stale_detail_never
import at.zanko.kalenderwecker.resources.stale_detail_since
import at.zanko.kalenderwecker.resources.stale_title
import at.zanko.kalenderwecker.resources.warning_scheduling
import at.zanko.kalenderwecker.ui.FormatTexts
import at.zanko.kalenderwecker.ui.localizedMessage
import at.zanko.kalenderwecker.ui.rememberFormatTexts
import org.jetbrains.compose.resources.stringResource
import at.zanko.kalenderwecker.ui.formatAlarmSubtitle
import at.zanko.kalenderwecker.ui.formatDayHeader
import at.zanko.kalenderwecker.ui.formatTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    banner: @Composable () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val zone = remember { TimeZone.currentSystemDefault() }
    val today = Clock.System.now().toLocalDateTime(zone).date
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it.localizedMessage())
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("KalenderWecker") },
                subtitle = { Text(stringResource(Res.string.home_subtitle)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(Res.string.settings))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            if (!state.hasUrl && state.sections.isEmpty()) {
                EmptyState(onOpenSettings)
            } else {
                DayList(state, zone, today, viewModel::setAlarmEnabled, banner)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayList(
    state: HomeUiState,
    zone: TimeZone,
    today: LocalDate,
    onToggle: (LocalDate, Boolean) -> Unit,
    banner: @Composable () -> Unit,
) {
    val texts = rememberFormatTexts()
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { banner() }
        state.schedulingError?.let { error ->
            item { WarningCard(stringResource(Res.string.warning_scheduling), error) }
        }
        if (state.scheduleStale) {
            item {
                WarningCard(
                    stringResource(Res.string.stale_title),
                    state.lastRefresh?.let {
                        stringResource(
                            Res.string.stale_detail_since,
                            formatDayHeader(it.toLocalDateTime(zone).date, today, texts),
                            formatTime(it, zone),
                        )
                    } ?: stringResource(Res.string.stale_detail_never),
                )
            }
        }
        item { NextAlarmCard(state.nextAlarm, state.offsetMinutes, zone, today, texts) }
        state.sections.forEach { section ->
            stickyHeader(key = "h-${section.date}") {
                DayHeader(section, state.offsetMinutes, zone, today, texts, onToggle)
            }
            items(section.appointments, key = { "a-${section.date}-${it.key}" }) {
                AppointmentCard(it, zone)
            }
        }
    }
}

@Composable
private fun WarningCard(title: String, detail: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NextAlarmCard(alarm: DayAlarm?, offsetMinutes: Int, zone: TimeZone, today: LocalDate, texts: FormatTexts) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = MaterialShapes.Sunny.toShape(),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (alarm != null) Icons.Rounded.Alarm else Icons.Rounded.AlarmOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(stringResource(Res.string.next_alarm), style = MaterialTheme.typography.labelLarge)
                if (alarm == null) {
                    Text(stringResource(Res.string.none_planned), style = MaterialTheme.typography.headlineMedium)
                } else {
                    Text(
                        "${formatDayHeader(alarm.date, today, texts)} · ${formatTime(alarm.time, zone)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(formatAlarmSubtitle(alarm, offsetMinutes, texts), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun DayHeader(
    section: DaySection,
    offsetMinutes: Int,
    zone: TimeZone,
    today: LocalDate,
    texts: FormatTexts,
    onToggle: (LocalDate, Boolean) -> Unit,
) {
    val alarm = section.alarm
    val container by animateColorAsState(
        if (alarm?.enabled == true) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh
    )
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(top = 16.dp, bottom = 4.dp)) {
        Text(
            formatDayHeader(section.date, today, texts),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        if (alarm != null) {
            Surface(color = container, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (alarm.source is AlarmSource.Fallback) Icons.Rounded.WbSunny else Icons.Rounded.Alarm,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            formatTime(alarm.time, zone),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(formatAlarmSubtitle(alarm, offsetMinutes, texts), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = alarm.enabled, onCheckedChange = { onToggle(section.date, it) })
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(appointment: Appointment, zone: TimeZone) {
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Column(Modifier.width(56.dp)) {
                Text(formatTime(appointment.start, zone), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                appointment.end?.let {
                    Text(formatTime(it, zone), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(displayTitle(appointment), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                appointment.description?.lineSequence()?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                appointment.location?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyState(onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = MaterialShapes.Cookie12Sided.toShape(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.size(120.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(Res.string.empty_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(Res.string.empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenSettings) { Text(stringResource(Res.string.empty_action)) }
    }
}

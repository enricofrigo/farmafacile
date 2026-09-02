package eu.frigo.farmafacile.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.frigo.farmafacile.domain.model.SyncProgress
import eu.frigo.farmafacile.presentation.screens.lists.PrivacyConsentDialog
import eu.frigo.farmafacile.presentation.theme.ExpiryWarningAmber
import eu.frigo.farmafacile.presentation.theme.ExpiryWarningAmberContainer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

    val lastUpdatedText = state.lastUpdatedTimestamp?.let {
        dateFormatter.format(Instant.ofEpochMilli(it))
    } ?: "Mai sincronizzato"

    val devicesLastUpdatedText = state.devicesLastUpdatedTimestamp?.let {
        dateFormatter.format(Instant.ofEpochMilli(it))
    } ?: "Mai sincronizzato"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. AIFA Database Section
            Text("Catalogo Farmaci AIFA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ultimo aggiornamento AIFA:", style = MaterialTheme.typography.bodyMedium)
                            Text(lastUpdatedText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("${state.totalCatalogCount} record", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }

                    if (state.isCatalogOutdated) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ExpiryWarningAmberContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ExpiryWarningAmber)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Catalogo AIFA più vecchio di 45 giorni.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ExpiryWarningAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.isSyncing) {
                        SyncProgressIndicator(
                            progress = state.aifaProgress,
                            defaultMessage = "Preparazione download AIFA..."
                        )
                    } else {
                        Button(
                            onClick = { viewModel.syncAifaCatalogNow() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aggiorna Catalogo AIFA Ora")
                        }
                    }
                }
            }

            HorizontalDivider()

            // 2. Medical Devices Section (Ministero della Salute)
            Text("Dispositivi Medici (Ministero della Salute)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ultimo aggiornamento Dispositivi:", style = MaterialTheme.typography.bodyMedium)
                            Text(devicesLastUpdatedText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("${state.totalDevicesCount} dispositivi", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.isSyncingDevices) {
                        SyncProgressIndicator(
                            progress = state.devicesProgress,
                            defaultMessage = "Preparazione download dispositivi medici..."
                        )
                    } else {
                        Button(
                            onClick = { viewModel.syncMedicalDevicesNow() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.MedicalServices, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aggiorna Catalogo Dispositivi Medici")
                        }
                    }
                }
            }

            HorizontalDivider()

            // 3. Expiry Notifications Section
            Text("Notifiche Scadenza", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Anticipo notifica prima della data di scadenza:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(7, 15, 30).forEach { days ->
                            FilterChip(
                                selected = state.expiryReminderDays == days,
                                onClick = { viewModel.setExpiryDays(days) },
                                label = { Text("$days giorni prima") }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // 4. Google Drive Sync & Privacy Section
            Text("Condivisione Google Drive & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Consenso Condivisione Sanitaria", fontWeight = FontWeight.Bold)
                            Text(
                                "Consente la sincronizzazione delle liste farmaci su Google Drive.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isSyncConsentGranted,
                            onCheckedChange = { granted ->
                                if (granted) {
                                    viewModel.openPrivacyDialog()
                                } else {
                                    viewModel.setConsent(false)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Risoluzione conflitti: strategia Last-Write-Wins sul singolo record in base al timestamp di ultima modifica.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    if (state.showPrivacyDialog) {
        PrivacyConsentDialog(
            onConsentGranted = { viewModel.setConsent(true) },
            onDismiss = { viewModel.closePrivacyDialog() }
        )
    }

    // Success / Error Feedback Dialog
    state.message?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearFeedback() },
            title = { Text("Operazione Completata") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearFeedback() }) {
                    Text("OK")
                }
            }
        )
    }

    state.errorMessage?.let { err ->
        AlertDialog(
            onDismissRequest = { viewModel.clearFeedback() },
            title = { Text("Errore") },
            text = { Text(err) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearFeedback() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun SyncProgressIndicator(
    progress: SyncProgress?,
    defaultMessage: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (progress) {
            is SyncProgress.Downloading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📥 Download in corso (${progress.downloadedMb} / ${progress.totalMb} MB)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${progress.percentageInt}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { progress.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            is SyncProgress.Importing -> {
                val fraction = progress.progressFraction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ Caricamento nel database: ${progress.formattedImportedCount} record",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    progress.percentageInt?.let { pct ->
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (fraction != null) {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            null -> {
                Text(
                    text = defaultMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

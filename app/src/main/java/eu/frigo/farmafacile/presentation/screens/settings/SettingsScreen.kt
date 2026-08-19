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
            // AIFA Database Section
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
                            Text("Ultimo aggiornamento:", style = MaterialTheme.typography.bodyMedium)
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
                                    "Catalogo più vecchio di 45 giorni.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ExpiryWarningAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.syncAifaCatalogNow() },
                        enabled = !state.isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download e importazione in corso...")
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aggiorna Catalogo AIFA Ora")
                        }
                    }
                }
            }

            Divider()

            // Expiry Notifications Section
            Text("Notifiche Scadenza Farmaci", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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

            Divider()

            // Google Drive Sync & Privacy Section
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

package eu.frigo.farmafacile.presentation.screens.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.frigo.farmafacile.domain.model.MedicineList
import eu.frigo.farmafacile.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    viewModel: ListsViewModel,
    onListSelected: (String) -> Unit,
    onNavigateToDosage: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FarmaFacile - Le Mie Liste", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToDosage) {
                        Icon(Icons.Default.Medication, contentDescription = "Dosi Oggi")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuova Lista")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Outdated Catalog Warning Banner
            if (state.isCatalogOutdated) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ExpiryWarningAmberContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Attenzione",
                            tint = ExpiryWarningAmber,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Catalogo farmaci non aggiornato",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "L'anagrafica AIFA non viene sincronizzata da più di 45 giorni.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (state.isCatalogSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            TextButton(onClick = { viewModel.syncCatalogNow() }) {
                                Text("Aggiorna", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (state.lists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nessuna lista presente. Creane una con il pulsante +")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.lists, key = { it.list.id }) { item ->
                        ListCard(
                            item = item,
                            onClick = { onListSelected(item.list.id) },
                            onShare = { viewModel.onShareListClicked(item.list) },
                            onDelete = { viewModel.deleteList(item.list.id) }
                        )
                    }
                }
            }
        }
    }

    // Create List Dialog
    if (state.showCreateDialog) {
        CreateListDialog(
            onDismiss = { viewModel.closeCreateDialog() },
            onConfirm = { name, desc -> viewModel.createList(name, desc) }
        )
    }

    // Privacy Consent Dialog for Google Drive
    if (state.showPrivacyConsentDialog) {
        PrivacyConsentDialog(
            onConsentGranted = { viewModel.onPrivacyConsentGranted() },
            onDismiss = { viewModel.onPrivacyConsentDismissed() }
        )
    }

    // Error Snackbar
    state.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Errore") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ListCard(
    item: ListWithStats,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.list.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.list.isShared) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Drive", style = MaterialTheme.typography.labelSmall) },
                            icon = { Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
                item.list.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${item.totalCount} farmaci",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (item.expiredCount > 0) {
                        Text(
                            text = "• ${item.expiredCount} scaduti",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ExpiryExpiredRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (item.criticalCount > 0) {
                        Text(
                            text = "• ${item.criticalCount} in scadenza",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ExpiryCriticalRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Condividi su Drive", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun CreateListDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova Lista Farmaci") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome lista (es. Casa, Viaggio)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione opzionale") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("Crea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun PrivacyConsentDialog(
    onConsentGranted: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Informativa Privacy e Condivisione") },
        text = {
            Text(
                "Attivando la condivisione su Google Drive, i dati relativi ai farmaci posseduti in questa lista (nomi dei medicinali, date di scadenza, dosaggi e note) verranno salvati su Google Drive in un file JSON dedicato e saranno accessibili a chiunque abbia accesso alla cartella condivisa.\n\n" +
                "Trattandosi di dati sanitari personali, ti chiediamo di confermare il tuo consenso esplicito prima di procedere."
            )
        },
        confirmButton = {
            Button(onClick = onConsentGranted) {
                Text("Accetto e Condividi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

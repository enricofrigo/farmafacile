package eu.frigo.farmafacile.presentation.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.frigo.farmafacile.core.utils.ExpiryUrgencyLevel
import eu.frigo.farmafacile.domain.model.UserMedicine
import eu.frigo.farmafacile.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    viewModel: ListDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: (String) -> Unit,
    onNavigateToAddManual: (String) -> Unit,
    onNavigateToEdit: (listId: String, medicineId: String) -> Unit,
    onNavigateToSyncLogs: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Farmaci Posseduti", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToSyncLogs(viewModel.listId) }) {
                        Icon(Icons.Default.History, contentDescription = "Log Sincronizzazione")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { onNavigateToAddManual(viewModel.listId) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Aggiungi Manualmente")
                }
                Spacer(modifier = Modifier.height(12.dp))
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToScanner(viewModel.listId) },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    text = { Text("Scansiona") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Cerca per nome, principio attivo o AIC...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Pulisci")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.activeFilter == FilterUrgency.ALL,
                    onClick = { viewModel.onFilterSelected(FilterUrgency.ALL) },
                    label = { Text("Tutti (${state.medicines.size})") }
                )
                FilterChip(
                    selected = state.activeFilter == FilterUrgency.CRITICAL_AND_EXPIRED,
                    onClick = { viewModel.onFilterSelected(FilterUrgency.CRITICAL_AND_EXPIRED) },
                    label = { Text("⚠️ Scaduti / <30gg") }
                )
                FilterChip(
                    selected = state.activeFilter == FilterUrgency.WARNING_30_90,
                    onClick = { viewModel.onFilterSelected(FilterUrgency.WARNING_30_90) },
                    label = { Text("⏳ 30-90 giorni") }
                )
                FilterChip(
                    selected = state.activeFilter == FilterUrgency.GOOD_OVER_90,
                    onClick = { viewModel.onFilterSelected(FilterUrgency.GOOD_OVER_90) },
                    label = { Text("✅ Buono (>90gg)") }
                )
            }

            if (state.filteredMedicines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.medicines.isEmpty())
                            "Nessun farmaco in questa lista.\nUsa il pulsante Scansiona per iniziare."
                        else "Nessun farmaco corrisponde ai filtri impostati.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredMedicines, key = { it.medicine.id }) { item ->
                        MedicineCard(
                            item = item,
                            onEdit = { onNavigateToEdit(viewModel.listId, item.medicine.id) },
                            onDelete = { viewModel.deleteMedicine(item.medicine.id) },
                            onQuantityChange = { delta -> viewModel.updateQuantity(item.medicine, delta) },
                            onOpenLeaflet = {
                                val url = viewModel.resolveLeafletUrl(item.medicine)
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                runCatching { context.startActivity(browserIntent) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MedicineCard(
    item: MedicineItemUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onOpenLeaflet: () -> Unit
) {
    val medicine = item.medicine
    val status = item.expiryStatus

    val (badgeBgColor, badgeTextColor, badgeLabel) = when (status.level) {
        ExpiryUrgencyLevel.EXPIRED -> Triple(ExpiryExpiredRedContainer, ExpiryExpiredRed, "SCADUTO")
        ExpiryUrgencyLevel.CRITICAL -> Triple(ExpiryExpiredRedContainer, ExpiryCriticalRed, "Scade tra ${status.daysRemaining} gg")
        ExpiryUrgencyLevel.WARNING -> Triple(ExpiryWarningAmberContainer, ExpiryWarningAmber, "Scade tra ${status.daysRemaining} gg")
        ExpiryUrgencyLevel.GOOD -> Triple(ExpiryGoodGreenContainer, ExpiryGoodGreen, "Scade: ${medicine.expiryDate ?: "N/D"}")
        ExpiryUrgencyLevel.UNKNOWN -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Scadenza non impostata")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    medicine.activeIngredient?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Expiry Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBgColor
                ) {
                    Text(
                        text = badgeLabel,
                        color = badgeTextColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Extra Info (AIC, Lot, Serial, Manual flag)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                medicine.aic?.let {
                    Text(
                        text = "AIC: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                medicine.lotNumber?.let {
                    Text(
                        text = "Lotto: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (medicine.isManualEntry) {
                    Text(
                        text = "✏️ Inserimento manuale",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Dosage times if configured
            medicine.dosageSchedule?.let { schedule ->
                if (schedule.isActive && schedule.times.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💊 Dosi: ${schedule.times.joinToString(", ")}" +
                                if (!schedule.instructions.isNullOrBlank()) " (${schedule.instructions})" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Action Row (Leaflet, Quantity stepper, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bugiardino Button
                OutlinedButton(
                    onClick = onOpenLeaflet,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bugiardino", style = MaterialTheme.typography.labelMedium)
                }

                // Quantity Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onQuantityChange(-1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Diminuisci")
                    }
                    Text(
                        text = "Qtà: ${medicine.quantity}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(
                        onClick = { onQuantityChange(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Aumenta")
                    }
                }

                // Delete Button
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

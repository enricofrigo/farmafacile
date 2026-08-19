package eu.frigo.farmafacile.presentation.screens.addedit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicineScreen(
    viewModel: AddEditMedicineViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.medicineId == null) "Aggiungi Farmaco" else "Modifica Farmaco",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = !state.isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Salva", tint = MaterialTheme.colorScheme.primary)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // General Info Section
            Text("Dati del Farmaco", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onNameChanged(it) },
                label = { Text("Nome Commerciale *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.activeIngredient,
                onValueChange = { viewModel.onActiveIngredientChanged(it) },
                label = { Text("Principio Attivo (es. Paracetamolo)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.aic,
                onValueChange = { viewModel.onAicChanged(it) },
                label = { Text("Codice AIC (9 cifre)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Expiry Date Picker Field
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    viewModel.onExpiryDateChanged(LocalDate.of(year, month + 1, dayOfMonth))
                },
                state.expiryDate?.year ?: calendar.get(Calendar.YEAR),
                state.expiryDate?.let { it.monthValue - 1 } ?: calendar.get(Calendar.MONTH),
                state.expiryDate?.dayOfMonth ?: calendar.get(Calendar.DAY_OF_MONTH)
            )

            OutlinedTextField(
                value = state.expiryDate?.toString() ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Data di Scadenza") },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Scegli Data")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.lotNumber,
                    onValueChange = { viewModel.onLotChanged(it) },
                    label = { Text("Lotto") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.serialNumber,
                    onValueChange = { viewModel.onSerialChanged(it) },
                    label = { Text("Seriale") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quantity stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quantità posseduta:", style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.onQuantityChanged(state.quantity - 1) }) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                    }
                    Text("${state.quantity}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { viewModel.onQuantityChanged(state.quantity + 1) }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }

            OutlinedTextField(
                value = state.leafletUrl,
                onValueChange = { viewModel.onLeafletUrlChanged(it) },
                label = { Text("Link Bugiardino PDF (opzionale)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.onNotesChanged(it) },
                label = { Text("Note personali (posologia, avvertenze)") },
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Daily Dosage Reminders Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Promemoria Assunzione Giornaliera", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Invia notifiche agli orari programmati", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = state.isDosageActive,
                    onCheckedChange = { viewModel.onDosageActiveChanged(it) }
                )
            }

            if (state.isDosageActive) {
                val timePickerDialog = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                        viewModel.addDosageTime(formattedTime)
                    },
                    8, 0, true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Orari configurati (${state.dosageTimes.size}):", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { timePickerDialog.show() }) {
                        Icon(Icons.Default.AddAlarm, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aggiungi Orario")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (time in state.dosageTimes) {
                        InputChip(
                            selected = true,
                            onClick = { viewModel.removeDosageTime(time) },
                            label = { Text(time) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "Rimuovi", modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = state.dosageInstructions,
                    onValueChange = { viewModel.onDosageInstructionsChanged(it) },
                    label = { Text("Istruzioni dose (es. 1 compressa a stomaco pieno)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Salva Farmaco", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Error Dialog
    state.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Attenzione") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

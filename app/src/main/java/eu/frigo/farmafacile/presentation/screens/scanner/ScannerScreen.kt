@file:OptIn(
    androidx.camera.core.ExperimentalGetImage::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package eu.frigo.farmafacile.presentation.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import eu.frigo.farmafacile.presentation.theme.ExpiryWarningAmber
import eu.frigo.farmafacile.presentation.theme.ExpiryWarningAmberContainer
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scansione DataMatrix GS1", fontWeight = FontWeight.Bold) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                // CameraX Preview and ML Kit Analyzer
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraExecutor = Executors.newSingleThreadExecutor()
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(
                                    Barcode.FORMAT_DATA_MATRIX,
                                    Barcode.FORMAT_QR_CODE,
                                    Barcode.FORMAT_ALL_FORMATS
                                )
                                .build()
                            val barcodeScanner = BarcodeScanning.getClient(options)

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                                        processImageProxy(barcodeScanner, imageProxy) { rawBarcode ->
                                            viewModel.onBarcodeScanned(rawBarcode)
                                        }
                                    }
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                // Camera binding failed
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay reticle guide
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    )
                    Text(
                        text = "Inquadra il codice DataMatrix della confezione",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 60.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } else {
                // Permission Denied View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Permesso Fotocamera Richiesto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Per scansionare i codici DataMatrix sulle confezioni dei farmaci è necessario concedere l'accesso alla fotocamera.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Concedi Permesso")
                    }
                }
            }

            // Scanned Medicine Modal Dialog
            if (state.showResultDialog) {
                ScanResultDialog(
                    state = state,
                    onManualNameChange = { viewModel.onManualNameChanged(it) },
                    onManualAiChange = { viewModel.onManualActiveIngredientChanged(it) },
                    onQuantityChange = { viewModel.onQuantityChanged(it) },
                    onNotesChange = { viewModel.onNotesChanged(it) },
                    onConfirm = { viewModel.saveMedicine() },
                    onDismiss = { viewModel.resumeScanning() }
                )
            }
        }
    }
}

private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { raw ->
                        onSuccess(raw)
                        return@addOnSuccessListener
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@Composable
fun ScanResultDialog(
    state: ScannerUiState,
    onManualNameChange: (String) -> Unit,
    onManualAiChange: (String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val parsed = state.scannedBarcodeData
    val aifa = state.matchedAifaMedicine
    val isMatched = aifa != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isMatched) "Farmaco Riconosciuto" else "Dati Scansionati",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isMatched) {
                    Text(
                        text = aifa!!.denominazione,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    aifa.principioAttivo?.let {
                        Text(text = "Principio attivo: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(text = "Forma/Confezione: ${aifa.descrizione}", style = MaterialTheme.typography.bodySmall)
                    aifa.ditta?.let {
                        Text(text = "Ditta: $it", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    // Manual entry warning badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ExpiryWarningAmberContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (parsed?.hasAic == true)
                                "⚠️ AIC non trovato a catalogo locale. Inserisci il nome manualmente."
                            else
                                "⚠️ Codice AIC assente nel codice a barre. Inserisci il nome manualmente.",
                            color = ExpiryWarningAmber,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = state.manualName,
                        onValueChange = onManualNameChange,
                        label = { Text("Nome del farmaco *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.manualActiveIngredient,
                        onValueChange = onManualAiChange,
                        label = { Text("Principio attivo (opzionale)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // GS1 Extracted Fields Summary
                parsed?.aic?.let {
                    Text(text = "Codice AIC: $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                parsed?.expirationDate?.let {
                    Text(text = "Data di scadenza: $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                parsed?.lotNumber?.let {
                    Text(text = "Lotto: $it", style = MaterialTheme.typography.bodySmall)
                }
                parsed?.serialNumber?.let {
                    Text(text = "Seriale: $it", style = MaterialTheme.typography.bodySmall)
                }

                // Quantity and Notes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quantità:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onQuantityChange(state.manualQuantity - 1) }) {
                            Icon(Icons.Default.Remove, contentDescription = null)
                        }
                        Text("${state.manualQuantity}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onQuantityChange(state.manualQuantity + 1) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                }

                OutlinedTextField(
                    value = state.manualNotes,
                    onValueChange = onNotesChange,
                    label = { Text("Note personali (opzionale)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                } else {
                    Text("Salva nella Lista")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

# FarmaFacile (MedTrack) - Gestione Domestica Farmaci & Dispositivi Medici

Applicazione Android nativa scritta in **Kotlin**, progettata per la gestione domestica dei farmaci e dei dispositivi medici tramite scansione di codici **GS1 DataMatrix**, consultazione dei **fogli illustrativi (bugiardini)**, gestione delle **scadenze** e delle **dosi giornaliere**, replica locale offline del catalogo pubblico **AIFA** e dell'elenco **Dispositivi Medici del Ministero della Salute (RDM)** e sincronizzazione selettiva delle liste su **Google Drive** con risoluzione conflitti **Last-Write-Wins (LWW)**.

Package ID: `eu.frigo.farmafacile`

---

## 📱 Funzionalità Principali

1. **Scansione GS1 DataMatrix (Farmaci & Dispositivi Medici)**:
   - Lettura istantanea tramite CameraX e Google ML Kit Barcode Scanning.
   - Parser generico GS1 conforme agli standard GS1 Healthcare / UDI.
   - Estrazione di AIC (AI 716, 9 cifre), GTIN / EAN (AI 01), Data di Scadenza (AI 17), Lotto (AI 10) e Seriale (AI 21).
   - **Ricerca a cascata**:
     1. Catalogo Farmaci AIFA (per codice AIC 9 cifre).
     2. Catalogo Dispositivi Medici Ministero della Salute (per Codice Catalogo Fabbricante / REF, numero RDM o GTIN).
     3. Fallback inserimento manuale assistito con pre-compilazione di lotto, seriale e data di scadenza.

2. **Gestione Liste Prodotti**:
   - Supporto a liste multiple personalizzate (es. *Casa*, *Viaggio*, *Armadietto Studio*).
   - Badge visivi colorati per l'urgenza di scadenza:
     - 🔴 **Rosso**: Scaduto o in scadenza entro 30 giorni.
     - 🟡 **Giallo/Arancio**: In scadenza tra 30 e 90 giorni.
     - 🟢 **Verde**: Scadenza superiore a 90 giorni.
   - Pulsante dedicato **Bugiardino** per visualizzare direttamente il foglio illustrativo PDF ufficiale (farmaci AIFA).

3. **Promemoria & Gestione Dosi**:
   - Notifiche locali per scadenze imminenti (anticipo configurabile: 30, 15 o 7 giorni).
   - Notifiche ricorrenti per l'assunzione delle dosi giornaliere con orari multipli (es. 08:00, 13:00, 20:00).
   - Azioni rapide direttamente dalla notifica: pulsanti **Assunto** e **Salta** che aggiornano il registro storico delle assunzioni senza aprire l'app.
   - Rischedulazione automatica di tutti gli allarmi al riavvio del dispositivo (`BOOT_COMPLETED`).

4. **Architettura Resiliente di Download & Parsing Streaming**:
   - **Disaccoppiamento Rete/Database**: il download ad alta velocità su file temporaneo (`cacheDir`) è separato dalla fase di decompressione e inserimento a database. Questo impedisce il *zero-window buffer overflow* e i *connection reset by peer* causati da socket TCP rallentati.
   - **Auto-Resume con HTTP Range (`Accept-Ranges: bytes`)**: in caso di disconnessione o caduta di rete durante il download (~56MB ZIP o ~82MB CSV), il client riprende automaticamente dallo specifico byte di interruzione (`Range: bytes=N-`) con retry esponenziale fino a 5 tentativi.
   - **Batch Transazionali Room**: inserimento a blocchi (1.000 - 2.000 record) per prevenire `OutOfMemoryError`. Pulizia automatica dei file temporanei a fine importazione.

5. **Condivisione Liste via Google Drive (Last-Write-Wins)**:
   - Autenticazione Google con scope ristretto `drive.file` (nessun accesso all'intero Drive dell'utente).
   - Esportazione e condivisione di singoli file JSON (`list_{listId}.json`) contenenti **esclusivamente** i prodotti posseduti di quella lista (mai i cataloghi pubblici completi).
   - Algoritmo di risoluzione conflitti **Last-Write-Wins (LWW)** a livello di singolo record basato su `updatedAt`.
   - Gestione delle cancellazioni tramite tombstones logici (`isDeleted = true`).
   - Registro storico dei conflitti e aggiornamenti (`sync_logs`), consultabile in qualsiasi momento.
   - Informativa e consenso privacy esplicito prima di abilitare la sincronizzazione di dati sanitari personali su cloud.

---

## 🏛️ Scelte Architetturali

Il progetto adotta la **Clean Architecture** abbinata al pattern **MVVM (Model-View-ViewModel)** e **Jetpack Compose (Material 3)**.

```
eu.frigo.farmafacile
├── core
│   ├── gs1               # Parser GS1 puro Kotlin (zero dipendenze Android)
│   └── utils             # Calcolo urgenza scadenze ed estensioni
├── domain
│   ├── model             # Modelli di dominio (UserMedicine, AifaMedicine, MedicalDevice, MedicineList, DoseLog, SyncLog)
│   ├── repository        # Interfacce di repository
│   └── usecase           # Logica di business pura (SyncConflictResolver, ParseGs1Barcode, ecc.)
├── data
│   ├── local             # Room Database (AifaCatalogDatabase e MedTrackUserDatabase), DAO ed entità
│   ├── remote            # ResilientFileDownloader, download AIFA e Dispositivi Medici
│   ├── repository        # Implementazioni concrete delle repository
│   ├── worker            # WorkManager CoroutineWorker per sync periodico
│   └── notifications     # NotificationHelper, AlarmScheduler e BroadcastReceiver
├── di                    # Moduli Hilt (AppModule, DatabaseModule, NetworkModule, RepositoryModule)
└── presentation
    ├── navigation        # NavGraph e rotte Compose
    ├── theme             # Colori, tipografia e tema Material 3
    ├── screens
    │   ├── lists         # Schermata principale liste
    │   ├── detail        # Dettaglio lista e inventario farmaci/dispositivi
    │   ├── scanner       # Scansione CameraX + ML Kit con auto-lookup a cascata
    │   ├── addedit       # Form inserimento/modifica e posologia
    │   ├── dosage        # Tracker dosi giornaliere
    │   ├── settings      # Stato cataloghi AIFA/Dispositivi e impostazioni
    │   └── sync          # Log di sincronizzazione e conflitti
    └── MainActivity.kt
```

### 💡 Motivazione: 2 Database Room Separati

1. **`AifaCatalogDatabase` (`aifa_catalog.db`)**:
   - Contiene la tabella `aifa_medicines` (~150.000 righe) e la tabella `medical_devices` (~1,7M righe registrate a repertorio).
   - È un database di sola lettura ad uso catalogo.
   - Può essere cancellato, rigenerato o ricostruito in streaming senza alcun rischio di lock o corruzione sui dati personali dell'utente.
2. **`MedTrackUserDatabase` (`medtrack_user.db`)**:
   - Contiene `medicine_lists`, `user_medicines`, `dose_logs` e `sync_logs`.
   - È un database leggero, compatto (< 1 MB), ad altissima velocità di lettura/scrittura.
   - Consente backup rapidi, export JSON per Google Drive e migrazioni Room indipendenti dal rilascio di nuovi dataset governativi.

---

## 🛠️ Requisiti di Compilazione ed Esecuzione

- **JDK**: Java 17 o Java 21 (Temurin / OpenJDK).
- **Android SDK**: `compileSdk 35`, `minSdk 26`, `targetSdk 35`.
- **Gradle**: 8.13 (incluso tramite `gradlew`).
- **Comandi principali**:
  ```bash
  # Esecuzione della suite completa di test unitari
  ./gradlew testDebugUnitTest --info

  # Compilazione dell'APK di debug
  ./gradlew assembleDebug
  ```

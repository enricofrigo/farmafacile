# FarmaFacile (MedTrack) - Gestione Domestica Farmaci

Applicazione Android nativa scritta in **Kotlin**, progettata per la gestione domestica dei farmaci tramite scansione di codici **GS1 DataMatrix**, consultazione dei **fogli illustrativi (bugiardini)**, gestione delle **scadenze** e delle **dosi giornaliere**, replica locale offline del catalogo pubblico **AIFA** e sincronizzazione selettiva delle liste su **Google Drive** con risoluzione conflitti **Last-Write-Wins (LWW)**.

Package ID: `eu.frigo.farmafacile`

---

## 📱 Funzionalità Principali

1. **Scansione GS1 DataMatrix**:
   - Lettura istantanea tramite CameraX e Google ML Kit Barcode Scanning.
   - Parser generico GS1 conforme agli standard GS1 Healthcare.
   - Estrazione di AIC (AI 716, 9 cifre), Data di Scadenza (AI 17), Lotto (AI 10), Seriale (AI 21) e GTIN (AI 01).
   - Auto-completamento automatico dei dettagli del farmaco interrogando il database locale AIFA.
   - Supporto all'inserimento manuale in caso di AIC assente o confezione estera/non a catalogo.

2. **Gestione Liste Farmaci**:
   - Supporto a liste multiple personalizzate (es. *Casa*, *Viaggio*, *Armadietto Studio*).
   - Badge visivi colorati per l'urgenza di scadenza:
     - 🔴 **Rosso**: Farmaco scaduto o in scadenza entro 30 giorni.
     - 🟡 **Giallo/Arancio**: In scadenza tra 30 e 90 giorni.
     - 🟢 **Verde**: Scadenza superiore a 90 giorni.
   - Pulsante dedicato **Bugiardino** per visualizzare direttamente il foglio illustrativo PDF ufficiale.

3. **Promemoria & Gestione Dosi**:
   - Notifiche locali per scadenze imminenti (anticipo configurabile: 30, 15 o 7 giorni).
   - Notifiche ricorrenti per l'assunzione delle dosi giornaliere con orari multipli (es. 08:00, 13:00, 20:00).
   - Azioni rapide direttamente dalla notifica: pulsanti **Assunto** e **Salta** che aggiornano il registro storico delle assunzioni senza aprire l'app.
   - Rischedulazione automatica di tutti gli allarmi al riavvio del dispositivo (`BOOT_COMPLETED`).

4. **Replica Locale Catalogo AIFA & Streaming Parser**:
   - Download periodico mensile tramite WorkManager o aggiornamento manuale immediato "Aggiorna Ora".
   - Parser streaming chunked (batch da 1.000 record) per elaborare in sicurezza il file CSV da ~82MB (~150.000 confezioni) senza incorrere in `OutOfMemoryError`.
   - Tracciamento della data di sincronizzazione e avviso in UI se il catalogo non viene aggiornato da oltre 45 giorni.

5. **Condivisione Liste via Google Drive (Last-Write-Wins)**:
   - Autenticazione Google con scope ristretto `drive.file` (nessun accesso all'intero Drive dell'utente).
   - Esportazione e condivisione di singoli file JSON (`list_{listId}.json`) contenenti **esclusivamente** i farmaci posseduti di quella lista (mai l'anagrafica AIFA).
   - Algoritmo di risoluzione conflitti **Last-Write-Wins (LWW)** a livello di singolo record farmaco, basato sul timestamp `updatedAt`.
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
│   ├── model             # Modelli di dominio (UserMedicine, AifaMedicine, MedicineList, DoseLog, SyncLog)
│   ├── repository        # Interfacce di repository
│   └── usecase           # Logica di business pura (SyncConflictResolver, ParseGs1Barcode, ecc.)
├── data
│   ├── local             # Room Database (AifaCatalogDatabase e MedTrackUserDatabase), DAO ed entità
│   ├── remote            # Download AIFA streaming e client Google Drive
│   ├── repository        # Implementazioni concrete delle repository
│   ├── worker            # WorkManager CoroutineWorker per sync mensile
│   └── notifications     # NotificationHelper, AlarmScheduler e BroadcastReceiver
├── di                    # Moduli Hilt (AppModule, DatabaseModule, NetworkModule, RepositoryModule)
└── presentation
    ├── navigation        # NavGraph e rotte Compose
    ├── theme             # Colori, tipografia e tema Material 3
    ├── screens
    │   ├── lists         # Schermata principale liste
    │   ├── detail        # Dettaglio lista e inventario farmaci
    │   ├── scanner       # Scansione CameraX + ML Kit
    │   ├── addedit       # Form inserimento/modifica farmaco e posologia
    │   ├── dosage        # Tracker dosi giornaliere
    │   ├── settings      # Stato catalogo AIFA e preferenze notifiche
    │   └── sync          # Log di sincronizzazione e conflitti
    └── MainActivity.kt
```

### 💡 Motivazione: 2 Database Room Separati

Nel progetto sono stati implementati **due database Room distinti**:

1. **`AifaCatalogDatabase` (`aifa_catalog.db`)**:
   - Contiene la tabella `aifa_medicines` (~150.000 righe, ~80 MB) e la tabella `catalog_metadata`.
   - È un database di sola lettura ad uso catalogo.
   - Può essere cancellato, rigenerato o ricostruito in streaming senza alcun rischio di corruzione o blocco sui dati personali dell'utente.
2. **`MedTrackUserDatabase` (`medtrack_user.db`)**:
   - Contiene `medicine_lists`, `user_medicines`, `dose_logs` e `sync_logs`.
   - È un database leggero, compatto (< 1 MB), ad altissima velocità di lettura/scrittura.
   - Consente backup rapidi, export JSON per Google Drive e migrazioni Room indipendenti dal rilascio di nuovi dataset AIFA.

---

## 🔍 Analisi e Assunzioni Verificate sui Dati Reali AIFA

Durante la fase di sviluppo sono stati analizzati direttamente i file CSV pubblicati da AIFA su `https://drive.aifa.gov.it/farmaci/`:

1. **File `confezioni_fornitura.csv`**:
   - **Dimensione**: circa 82 MB.
   - **Separatore**: punto e virgola (`;`), con campi racchiusi tra virgolette doppie (`"`).
   - **Struttura intestazione**:
     ```csv
     "CODICE_AIC";"COD_FARMACO";"COD_CONFEZIONE";"DENOMINAZIONE";"DESCRIZIONE";"CODICE_DITTA";"RAGIONE_SOCIALE";"STATO_AMMINISTRATIVO";"TIPO_PROCEDURA";"FORMA";"CODICE_ATC";"PA_ASSOCIATI";"FORNITURA";"LINK_FI";"LINK_RCP"
     ```
   - `CODICE_AIC`: stringa a 9 cifre (es. `"000367045"`).
   - `PA_ASSOCIATI`: principio attivo del farmaco (es. `"SENNA FOGLIA"`, `"PARACETAMOLO"`).
   - `LINK_FI`: link diretto all'endpoint REST AIFA che restituisce direttamente lo stream PDF del Foglio Illustrativo (es. `https://api.aifa.gov.it/aifa-bdf-eif-be/1.0.0/organizzazione/2934/farmaci/367/stampati?ts=FI`).

2. **Risoluzione URL Bugiardino & Fallback**:
   - Se `LINK_FI` è popolato, l'app apre direttamente lo stream PDF del foglio illustrativo ufficiale.
   - Se `LINK_FI` è assente o il farmaco è stato inserito manualmente, l'app adotta come fallback l'apertura del portale di ricerca [Banca Dati Farmaci AIFA](https://medicinali.aifa.gov.it/) o la ricerca sul codice AIC, documentando chiaramente nel codice (`ListDetailViewModel.kt`) questa incertezza tramite apposito commento `TODO`.

---

## 📦 Specifiche Parser GS1 DataMatrix

Il parser GS1 (`Gs1DataMatrixParser.kt`) è implementato come classe pura Kotlin, senza dipendenze dal framework Android, ed è coperto da test unitari:

- **Campi a lunghezza fissa**:
  - `(01)` GTIN: 14 cifre.
  - `(17)` Data di Scadenza: 6 cifre (`YYMMDD`). Supporta la convenzione GS1 `YYMM00` (ultimo giorno del mese) e calcolo bisestile.
  - `(716)` Codice AIC: 9 cifre in chiaro senza conversioni base32.
- **Campi a lunghezza variabile**:
  - `(10)` Numero di Lotto: alfanumerico fino a 20 caratteri, delimitato dal carattere `GS` (ASCII 29, `\u001D`) o da fine stringa.
  - `(21)` Numero di Seriale: alfanumerico fino a 20 caratteri, delimitato da `GS` o fine stringa.
- **Formati supportati**: stringhe grezze con separatore `GS`, stringhe con prefisso ISO `]d2`, stringhe formattate con parentesi `(01)...(17)...(716)...`.
- **Assenza di AI 716**: il parser restituisce comunque scadenza, lotto, seriale e GTIN, impostando `aic = null` e `hasAic = false` affinché l'interfaccia proponga l'inserimento manuale del nome farmaco conservando i dati estratti.

---

## 🔄 Algoritmo Last-Write-Wins (LWW) per la Sincronizzazione

La sincronizzazione su Google Drive condivide esclusivamente un file JSON `list_{listId}.json`.

```
[Record Locale]  vs  [Record Remoto]
       │                    │
       ├─ updatedAt Remoto > updatedAt Locale ──> Sovrascrive locale e registra in sync_logs
       ├─ updatedAt Locale > updatedAt Remoto ──> Mantiene locale (sarà inviato al prossimo upload)
       ├─ Record Remoto è isDeleted = true ────> Esegue soft-delete locale con log
       └─ Record nuovo (locale assente) ───────> Inserisce nel DB locale con log
```

Ogni sovrascrittura o inserimento automatico genera una riga nella tabella `sync_logs`, visualizzabile dall'utente nella schermata *Log Sincronizzazione*.

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

---

## 🛡️ Gestione Permessi & Errori

- **Permesso Fotocamera**: richiesta runtime all'apertura dello scanner con schermata informativa in caso di rifiuto.
- **Permesso Notifiche (Android 13+)**: richiesta all'avvio per abilitare i canali ad alta priorità di scadenze e dosi.
- **Offline Mode**: se il dispositivo è offline durante il sync AIFA o Drive, l'app notifica l'errore senza bloccare le funzionalità offline del catalogo già salvato.

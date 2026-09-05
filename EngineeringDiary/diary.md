# ARTHIX — Engineering Diary & Implementation Log

> **Repository:** `ARTHIX` (Zero-Typing Smart Personal Finance Tracker for India's UPI Economy)  
> **Target Platform:** Android (Kotlin, Jetpack Compose, Room + SQLCipher, Dagger Hilt)  
> **Document Purpose:** Complete chronological log of features built, architectural choices made, discarded experiments, underlying infrastructure/models, and major engineering hurdles solved.

---

## 📖 Table of Contents
1. [Project Overview & Core Philosophy](#1-project-overview--core-philosophy)
2. [Chronological Implementation Diary](#2-chronological-implementation-diary)
3. [What We Implemented vs. What We Discarded / Refactored](#3-what-we-implemented-vs-what-we-discarded--refactored)
4. [Infrastructure & Models Overview](#4-infrastructure--models-overview)
5. [Major Engineering Hurdles & How We Sorted Them](#5-major-engineering-hurdles--how-we-sorted-them)
6. [Current Architecture & Feature Status](#6-current-architecture--feature-status)

---

## 1. Project Overview & Core Philosophy

Traditional personal finance trackers suffer from a **90% abandonment rate** because manual typing for 5–10 daily micro-transactions (chai, auto, groceries) imposes too much friction. Meanwhile, automated bank SMS readers capture the exact amount but lack human context (a payment to `merchant4829@icici` doesn't tell you if it was lunch or medicine).

**ARTHIX bridges this gap with a single core principle:**
> **"Deterministic Core + Gesture Accelerant"**  
> Bank notifications provide authoritative mathematical truth (amount, payee, timestamp). Physical gestures (shake) and multimodal inputs (voice, camera OCR) provide human intent and categorization without typing. The system operates **100% on-device**, ensuring complete financial privacy with zero cloud dependencies.

---

## 2. Chronological Implementation Diary

### Phase 0: Foundations, Data Layer & Core Contracts
- **What was done:**
  - Defined paise-precision financial entities (`TransactionEntity`, `PendingCaptureEntity`, `PendingNotificationEntity`, `ReportEntity`, `SplitRecordEntity`, `SplitParticipantEntity`).
  - Integrated **Room Database** backed by **SQLCipher 256-bit AES encryption at rest** and Android Keystore.
  - Set up **Dagger Hilt** dependency injection and cross-module repository contracts (`TransactionRepository`, `SplitRepository`, `ReportRepository`).
- **Design Decision:** Fixed all currency values to **64-bit integer paise** (₹1.00 = 100 paise) to permanently eliminate IEEE 754 floating-point rounding inaccuracies.

---

### Phase 1: Physical Motion Sensing & Shake Detection
- **What was done:**
  - Ingested raw sensor data from the hardware Accelerometer.
  - Implemented a dual-peak acceleration sign-inversion algorithm: detects sharp directional oscillation along the dominant movement axis while filtering out walking or gravity drift.
  - Added tactile haptic vibration confirmation on gesture registration.
- **Outcome:** Reliable double-shake detection with low false-positive rates.

---

### Phase 2: Ingestion Pipeline & Bank SMS Parsing
- **What was done:**
  - Created a high-throughput transaction ingestion pipeline.
  - Built an Indian banking regex parser supporting standard DLT 3-part alphanumeric sender headers (e.g., `VK-HDFCBK`, `AD-ICICIB`, `BOBSMS`).
  - Extracted transaction direction (debit/credit), amount in paise, payee name, and timestamp.
- **Outcome:** Instant parsing of incoming transactional messages without cloud processing.

---

### Phase 3: Android Notification Listening & Event Reconciliation
- **What was done:**
  - Implemented `NotificationListenerService` capturing push notifications from Google Pay, PhonePe, Paytm, and banking apps.
  - Developed the **Reconciliation Engine**: a temporal correlation engine with a **symmetric 120-second matching window**.
  - Whether the user shakes the phone *before* or *after* the payment notification arrives, the engine pairs the intent with the bank alert, merges category + exact amount, and logs the transaction.
- **Outcome:** Zero-typing transaction logging in under 3 seconds after payment.

---

### Phase 4: Multimodal Inputs — Camera OCR & Voice AI
- **What was done:**
  - **Camera OCR (`com.chirag.arthix.ocr`):** CameraX pipeline with on-device **Google ML Kit Text Recognition v2**. Scans paper bills/receipts for "Total", "Grand Total", and "Amount Payable" with sanity bounds (₹1 to ₹50,000), routing results into manual prefill.
  - **Voice AI Engine (`com.chirag.arthix.voice`):** Speech-to-text integration with spoken amount parsing ("four fifty" $\rightarrow$ ₹450, "twelve hundred" $\rightarrow$ ₹1,200), discard intent matching ("skip", "cancel"), and category taxonomy mapping.
  - Designed `VoiceCaptureBottomSheet` with real-time waveform pulsation feedback.
- **Outcome:** Hands-free voice capture and instant paper receipt logging.

---

### Phase 5: Deterministic Financial Intelligence & PDF Reports
- **What was done:**
  - **Deterministic Computation:** Pure integer arithmetic for monthly category totals, net savings, budget burns, and top spenders.
  - **Anti-Naive Projection Anchoring:** Blends current spend rate with historical baseline weighted by elapsed time ($w = d / 7$) to prevent unrealistic month-end predictions early in the cycle.
  - **Grounding Safeguard:** A strict `GroundingValidator` regex scanner ensures that no AI or template output can display numbers that weren't verified by the computation engine.
  - **Native PDF Export:** Generates clean, downloadable financial PDF statements using native Android `Canvas` and `PdfDocument` graphics.
- **Outcome:** Provably accurate financial reports with zero AI math hallucinations.

---

### Phase 6: Modernized Split-Bill Engine & SMS Reminders
- **What was done:**
  - Interactive Compose UI featuring vertical sliding **puck cylinders** with drag-and-drop share adjustment, percentage gauges, and equal/custom split modes.
  - Integrated direct `SmsManager` silent reminder dispatch: sends personalized SMS messages to participants stating their exact share and payer name with default `+91` country code handling.
- **Outcome:** Split bills created and reminders dispatched to group members in seconds.

---

### Phase 7: App Security & Access Control
- **What was done:**
  - Added an internal App Lock layer supporting both **PIN (4–6 digits)** and **Pattern** authentication.
  - Configured lifecycle re-locking: re-locks automatically when the app is backgrounded or device goes to sleep.
  - Stored credentials using salted cryptographic hashes in encrypted preferences.
- **Outcome:** Privacy protection for financial logs if the phone is unlocked or shared.

---

### Phase 8: On-Device AI Goal Planner
- **What was done:**
  - Developed `AiGoalPlannerEngine` for intelligent financial target calculation.
  - Analyzes monthly income, non-discretionary expenses, and free cash flow to evaluate feasibility, recommend realistic monthly contribution amounts, and calculate emergency fund safety buffers.
  - Added a uniform **Goals** entry button in the Plus floating menu.
- **Outcome:** Clear financial milestones and feasibility timelines calculated entirely on-device.

---

### Phase 9: Close Friends, Speech-to-SMS Linking, Indian Phonetic Normalization & Paid Filter Fix
- **What was done:**
  - **Close Friends Storage:** Created encrypted local `CloseFriendEntity` table (Room version 7) storing names, phone numbers, and optional aliases.
  - **Settings UI:** Added dedicated Close Friends management tab with Add/Edit/Delete actions and default `+91` phone validation.
  - **Quick-Access Split Chips:** Added horizontal scrolling chips (`+ Name` / `✓ Name`) at the top of the Split Bill screen for one-tap participant toggling with phone pre-filled.
  - **Speech-to-SMS Reminder Linking:** When participants are added via speech (voice split intent or voice mic capture), ARTHIX matches them against Close Friends, attaches their phone number, and flags them as eligible recipients for silent SMS reminders via `SmsManager`.
  - **Indian English Phonetic Normalization (`normalizePhonetic`):** Solves common STT transcription discrepancies for Indian names without requiring users to configure pet names/aliases. Maps vowel/consonant alternations (`ee` $\leftrightarrow$ `i`, `oo` $\leftrightarrow$ `u`, `aa` $\leftrightarrow$ `a`, `w` $\leftrightarrow$ `v`, `ph` $\leftrightarrow$ `f`) and collapses duplicate consonants (`mm` $\rightarrow$ `m`, `rr` $\rightarrow$ `r`) so spoken `"neeru"` automatically resolves to saved friend `"Niru"`, `"pooja"` to `"Puja"`, and `"amman"` to `"Aman"`.
  - **Action Phrase Stripping:** Filters out spoken action tokens (`logged`, `log`, `add`, `split`, `record`) from voice input so phrases like `"neeru logged"` or `"logged neeru"` cleanly extract candidate `"Neeru"` $\rightarrow$ matched to `"Niru"`.
  - **First-Time Save Prompt:** Prompts user to save new participants to Close Friends after completing a split.
  - **Paid-Status Bug Fix:** Strictly excluded participants already marked as `isPaid` from receiving SMS reminders in both ViewModel and `SplitSmsReminderManager`.
- **Outcome:** Natural voice-driven split creation that understands phonetic variations, automatically retrieves phone numbers, and links directly to automated SMS debt reminders.

---

## 3. What We Implemented vs. What We Discarded / Refactored

| Feature / Subsystem | What Was Implemented | What Was Discarded / Refactored | Rationale |
| :--- | :--- | :--- | :--- |
| **AI / Intelligence Layer** | On-device models (Whisper ONNX, ML Kit, deterministic rule heuristics) | Cloud LLMs (OpenAI API, Claude API, external webhooks) | Financial data must never leave the user's phone. Cloud calls introduce latency and fail when offline. |
| **Notification Capture** | `SYSTEM_ALERT_WINDOW` floating category overlay on top of payment apps | Action buttons inside Android notifications tray | When paying via UPI, 3–4 bank/UPI notifications arrive at once, burying tray buttons before the user can tap them. |
| **Motion Sensing** | Bounded `CaptureGraceWindowService` (10s on app switch, up to 120s max) | 24/7 background sensor listener | Keeping accelerometer hardware active around the clock drains phone battery in hours. |
| **Speech-to-Text** | Dual engine: Platform `SpeechRecognizer` + quantized OpenAI Whisper Tiny ONNX | Heavy full-size VOSK acoustic models | Whisper int8 ONNX offers superior accuracy for Indian English accents with smaller footprint (~39MB). |
| **Report Phrasing** | Deterministic math engine + `GroundingValidator` numeric whitelist | Free-form generative LLM text output | LLMs hallucinate numbers in financial summaries; every displayed rupee figure must be mathematically grounded. |
| **Split Reminders** | Direct silent send via Android `SmsManager` | Manual `ACTION_SENDTO` intent app redirects | Launching the Messages app for each participant forces 4–5 manual screen switches for a group split. |
| **Split Triggering** | On-demand trigger via Plus Menu or Quick Action | Automatic modal prompt on *every* single transaction commit | Prompting to split on daily solo purchases (metro, grocery) was intrusive and annoying. |
| **Contacts Access** | Dedicated local "Close Friends" list with custom aliases | Full Android Contacts permission scan | Scanning the entire address book requests invasive permissions; users split bills with only 3–5 close peers. |
| **Currency Handling** | 64-bit integer arithmetic in paise (`Long`) | Floating point numbers (`Double`, `Float`) | IEEE 754 floating point arithmetic introduces rounding errors like ₹49.9999999 in financial totals. |

---

## 4. Infrastructure & Models Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           ARTHIX APP ARCHITECTURE                       │
├──────────────────────────────────┬──────────────────────────────────────┤
│  PRESENTATION LAYER              │  Jetpack Compose + Material 3        │
│                                  │  MVI / StateFlow + Clean Navigation   │
├──────────────────────────────────┼──────────────────────────────────────┤
│  DOMAIN & RECONCILIATION         │  Temporal Event Correlation (120s)   │
│                                  │  Soundex & Levenshtein Name Matcher  │
│                                  │  Deterministic Paise Math Engine     │
├──────────────────────────────────┼──────────────────────────────────────┤
│  ON-DEVICE ML & SENSORS          │  OpenAI Whisper Tiny (int8 ONNX)     │
│                                  │  Google ML Kit Text Recognition v2   │
│                                  │  Hardware Accelerometer + Haptics    │
├──────────────────────────────────┼──────────────────────────────────────┤
│  PERSISTENCE & SECURITY          │  Room Database v7 + SQLCipher AES-256│
│                                  │  Android Keystore + EncryptedPrefs   │
└──────────────────────────────────┴──────────────────────────────────────┘
```

### 1. Data & Storage Infrastructure
- **Room SQLite + SQLCipher:** All database tables (`transactions`, `splits`, `close_friends`, `reports`) are encrypted on disk with 256-bit AES encryption. Keys are managed via Android Keystore.
- **Paise-Level Math:** All monetary figures are stored and calculated as integer `Long` paise ($1\text{ INR} = 100\text{ paise}$), formatted to decimals only at render time.

### 2. On-Device AI / ML Models
- **Speech Recognition (STT):**
  - Primary: Platform `SpeechRecognizer` for instantaneous native voice recognition.
  - Fallback: Quantized **OpenAI Whisper Tiny** (`int8.onnx`, ~39MB) running through `sherpa-onnx` on CPU using 80-dimensional log-mel filterbanks.
- **Vision OCR:**
  - **Google ML Kit Text Recognition v2** (bundled Latin script model, runs 100% offline, zero network requests).
- **Phonetic & Fuzzy Matching:**
  - Standard Soundex algorithm generating 4-character phonetic keys (e.g., `N600` for both "Niru" and "Neeru").
  - Levenshtein matrix distance calculation with a threshold of $\le 2$ for handling typos.

### 3. Background Services & Native APIs
- **`NotificationListenerService`:** Captures system notifications from financial packages (`com.google.android.apps.nbu.paisa.user`, `net.one97.paytm`, `com.phonepe.app`).
- **`SYSTEM_ALERT_WINDOW`:** Displays floating Compose overlay over third-party apps for quick 5-second category tagging.
- **`SmsManager`:** Dispatches automated group reminder SMS messages silently.

---

## 5. Major Engineering Hurdles & How We Sorted Them

### 1. The Notification Contention Flaw
- **The Hurdle:** Initially, transaction categorization was attempted via action buttons on Android notifications. However, when paying at a merchant, 3 to 4 notifications arrive simultaneously (bank debit SMS, GPay confirmation, merchant receipt), pushing ARTHIX's notification out of view before the user can tap.
- **How We Sorted It:** Abandoned tray notifications in favor of a **floating system overlay (`SYSTEM_ALERT_WINDOW`)** that appears directly on top of the payment app for 5 seconds with an animated countdown bar and auto-collapses into a non-intrusive edge badge.

---

### 2. Battery Drain from Continuous Sensor Monitoring
- **The Hurdle:** Listening to accelerometer events 24/7 to detect shakes causes unacceptable battery drain, causing the Android OS to kill the app.
- **How We Sorted It:** Created `CaptureGraceWindowService`. The accelerometer sensor is only active during a **bounded grace window** (10 seconds when switching apps or shaking, extendable up to 120 seconds upon user interaction). Once the window expires, the service self-terminates.

---

### 3. Asynchronous Race Conditions between Gestures and SMS
- **The Hurdle:** In real life, users might shake their phone *before* opening the payment app, or the bank SMS might take 30 to 45 seconds to arrive due to carrier delays. A simple lock-step match fails if the events arrive out of order.
- **How We Sorted It:** Built a **symmetric nearest-neighbor reconciliation engine**. Both shake events and incoming notifications are stored in temporary pending queues with timestamps. The engine correlates events bidirectional within a 120-second rolling window, pairing the human intent with the verified bank amount regardless of arrival order.

---

### 4. Spoken Indian Names, Accents & Speech-to-SMS Linking
- **The Hurdle:** When users use voice speech to log splits (e.g. saying *"split 600 with neeru"* or *"neeru logged"*), standard speech-to-text engines transcribe names with alternate phonetic spellings (transcribing "Niru" as "Neeru", "Puja" as "Pooja", "Aman" as "Amman"). Furthermore, users often attach action words (e.g. *"neeru logged"*), causing exact string lookups to fail. Crucially, if speech fails to match the saved Close Friend, their phone number is never retrieved, breaking the automated SMS debt reminder messaging pipeline.
- **How We Sorted It:**
  1. **Rule-Based Indian Phonetic Normalizer (`normalizePhonetic`):** Maps common vowel/consonant alternations (`ee` $\leftrightarrow$ `i`, `oo` $\leftrightarrow$ `u`, `aa` $\leftrightarrow$ `a`, `w` $\leftrightarrow$ `v`, `ph` $\leftrightarrow$ `f`) and collapses duplicate consonants (`mm` $\rightarrow$ `m`, `rr` $\rightarrow$ `r`). Both `"neeru"` and `"niru"` normalize identically to `"niru"`, achieving 100% deterministic matching without requiring manual nickname aliases.
  2. **Action Phrase Stripping:** Filters out action verbs (`logged`, `log`, `record`, `add`, `split`, `bill`) from candidates so utterances like `"neeru logged"` cleanly resolve to `"Niru"`.
  3. **Direct Speech-to-SMS Link:** Once resolved, the participant's phone number is automatically populated from `CloseFriendEntity.phoneNumber`. This marks the participant as eligible for `SplitSmsReminderManager.sendSplitReminders`, so an SMS reminder is sent directly to them when the split is saved.
  4. **Multi-Tier Fallback Resolver:**
     - Tier 1: Exact Name match
     - Tier 2: Indian Phonetic Normalization (`normalizePhonetic`)
     - Tier 3: Optional user-configured Aliases
     - Tier 4: Soundex phonetic key comparison
     - Tier 5: Levenshtein edit distance fallback ($\le 2$)
  5. **Visual Confirmation:** Every match displays a green `"✓ Matched [Name]"` chip on the participant cylinder so the user can verify or dismiss.

---

### 5. Accidental SMS Reminders to Paid Participants
- **The Hurdle:** When settling group expenses, participants who already paid cash or transferred money upfront were still receiving automated SMS debt reminders, causing confusion.
- **How We Sorted It:** Implemented a two-tier strict filter: `!it.isPaid` checks at both the UI ViewModel layer and the low-level `SplitSmsReminderManager` dispatch layer, ensuring zero SMS messages can ever be dispatched to marked-as-paid members.

---

### 6. Local JVM Test Execution with Java 21 & JNI Classes
- **The Hurdle:** In Java 21, Byte Buddy / Mockito inline agent failed with `MockitoException: Could not modify all classes [class java.lang.Object, class WhisperSttEngine]` when trying to mock concrete classes and Android context in unit tests.
- **How We Sorted It:** Replaced fragile bytecode mocking with **clean secondary constructors** and lightweight test instances (`WhisperSttEngine()`, `SplitGroupSuggestionHeuristic()`). Added null-safe degradation so unit tests run at maximum speed without requiring dynamic agent attachments or Robolectric overhead.

---

## 6. Current Architecture & Feature Status

| Area | Status | Key Highlights |
| :--- | :---: | :--- |
| **Shake Capture & Overlay** | ✅ Complete | Dual-peak gesture detection + 5s auto-collapse floating pill |
| **Bank SMS & Notif Ingestion** | ✅ Complete | Unified DLT header parsing + 120s symmetric reconciliation |
| **Voice AI & Camera OCR** | ✅ Complete | Whisper int8 ONNX + ML Kit v2 receipt recognition |
| **Deterministic Reports & PDF**| ✅ Complete | 100% integer paise math + Grounding Safeguard + Native PDF |
| **Split-Bill Engine** | ✅ Complete | Vertical sliding puck UI + SMS Manager reminders + +91 default |
| **Close Friends & Matching** | ✅ Complete | Encrypted local table + quick-add chips + Soundex phonetic matching |
| **Internal App Lock** | ✅ Complete | PIN & Pattern protection + auto lifecycle re-lock |
| **AI Goal Planner** | ✅ Complete | Cash flow feasibility analysis + emergency fund sizing |
| **Local Encrypted Storage** | ✅ Complete | Room v7 + SQLCipher AES-256 + zero cloud dependencies |

---

*Log generated for ARTHIX development repository. All features verified on branch `fvc1`.*

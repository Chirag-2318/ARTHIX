# ARTHIX

Zero-typing expense tracker for India's UPI economy. Bank notifications supply the ground truth (amount, payee, time); a phone shake supplies the categorization. No typing, no manual entry, nothing leaves the device.

Native Android · Kotlin · Jetpack Compose · Room + SQLCipher · Dagger Hilt

---

## The problem

Personal finance apps in India fail for a specific reason: UPI produces 5–10 micro-transactions a day (chai, auto, groceries), and typing each one into an app is enough friction that people quit within weeks. Automated SMS/notification readers solve half the problem — they get the exact amount — but a debit to `merchant4829@icici` doesn't tell you if it was lunch or medicine. The amount is correct and the context is missing, every time.

ARTHIX's answer: don't ask the user to enter data the bank already sent. Ask for the one bit of information the bank *can't* send — what the money was for — and get it through the cheapest possible gesture, a shake, correlated after the fact with whichever notification actually matches it.

## How it works

```
Payment happens
      │
      ├── Bank/UPI app fires a notification  ──────┐
      │        (GPay, PhonePe, Paytm, BHIM, Cred)   │
      │                                              ▼
      └── User shakes the phone            Reconciliation Engine
               (before or after,           nearest-neighbour match,
                doesn't matter)             120s symmetric window
                                                      │
                                                      ▼
                                          Transaction logged with
                                          category + exact amount
                                          < 3 seconds after payment
```
<img width="1536" height="1024" alt="arc" src="https://github.com/user-attachments/assets/3e372d1d-fa14-48c8-b62d-bf0a6666dfdb" />

The two signals are captured independently and matched afterward, which is what makes the ordering not matter. A user can shake before opening the payment app, or the bank SMS can lag 30–45 seconds behind the debit — both are common in practice, and a naive lock-step pairing breaks on either. Instead, both shakes and notifications land in timestamped pending queues, and a nearest-neighbour matcher pairs them from either direction within the window. If two candidates are close enough in time to be ambiguous, ARTHIX shows a one-tap disambiguation prompt rather than guessing silently.

Voice and camera OCR exist as fallback capture paths for the cases a shake doesn't cover — logging a cash purchase, or a paper receipt with no digital notification at all.

## What's actually in the box

| Layer | Implementation | Why |
|---|---|---|
| **Motion sensing** | Dual-peak accelerometer oscillation detector (≥2 direction reversals in a 500ms rolling window) | Filters walking and pocket movement without needing a second sensor |
| **Sensor lifecycle** | `CaptureGraceWindowService` — accelerometer active only in a bounded 10–120s grace window, then self-terminates | A 24/7 accelerometer listener drains the battery in hours; this doesn't |
| **Notification capture** | `NotificationListenerService` with a strict package allow-list | No screenshot access, no cloud OCR of your bank alerts, no permissions beyond what's needed |
| **Reconciliation** | Nearest-neighbour temporal matcher, symmetric 120s window, single-threaded serial coroutine dispatcher | Shake-then-pay and pay-then-shake both need to resolve to the same transaction |
| **Currency** | Every amount stored and computed as `Long` paise, never `Double`/`Float` | Floating point in a finance app eventually shows you ₹49.9999999; integers don't |
| **Reports** | Deterministic integer-math engine computes every number; an on-device LLM only phrases the sentence around numbers that already exist | The LLM is not allowed to do arithmetic. A regex whitelist scanner checks every numeric token the model outputs against the pre-computed values and falls back to a template if anything is ungrounded |
| **Voice** | Whisper tiny.en, quantized int8 ONNX, via Sherpa — runs on-device | Accurate enough for Indian-accented speech at ~39MB, no network round-trip |
| **OCR** | CameraX + ML Kit Text Recognition v2 | Reads "Total" / "Amount Payable" off paper receipts, offline |
| **Storage** | Room (WAL mode) encrypted with SQLCipher (AES-256), keys in Android Keystore | Nothing is synced anywhere; the encryption is for a lost or shared phone, not a server breach — there is no server |

Everything above runs on-device. There is no backend, no account creation, no cloud sync. That's a design constraint, not a limitation to be fixed later: an app that reads your bank notifications and phrases your spending report has no legitimate reason to phone home, and building it that way removes an entire category of privacy questions rather than answering them.

## Splitting bills

Group expenses get a Compose UI with draggable per-person shares (snapped to whole rupees, not raw pixel fractions — early versions produced amounts like ₹127.34, which nobody wants to owe). Participants can be added by voice, matched against a local, encrypted "Close Friends" list rather than pulling the full Android contacts permission — most people split bills with the same 3–5 people, so scanning an entire address book to find them is a worse trade than it looks.

Voice-added names go through a fallback chain before anything is asked of the user:

1. Exact match
2. Indian-English phonetic normalization (`ee`↔`i`, `oo`↔`u`, `w`↔`v`, `ph`↔`f`, collapsed double consonants) — resolves "neeru" and "niru" to the same person
3. User-configured aliases
4. Soundex phonetic key
5. Levenshtein distance ≤ 2

Once a participant resolves to a saved contact, their number is pulled automatically and they become eligible for an SMS reminder — sent directly via `SmsManager`, not by handing off to the Messages app once per participant. Anyone already marked as paid is filtered out of the reminder list at two separate layers, so a settled debt can't accidentally get chased.

## Why not just call an LLM API for all of this

Because the two things this app touches most — your bank notifications and your spending totals — are the two things it's least acceptable to get wrong or leak. A cloud LLM adds latency, requires network, and turns "the app knows my last ten transactions" into a request that leaves the phone. The rule here is one line: **the LLM is never the source of a number that appears on screen.** Everything numeric is computed in plain, testable, deterministic code first; language generation is bolted on afterward and checked against the math, not trusted.

## Project status

Core capture, reconciliation, reporting, splitting, and app-lock are built and working end to end. What's genuinely unverified right now:

- Reliability of the background notification listener across aggressive OEM battery managers (MIUI, ColorOS) — not yet tested on physical devices from those manufacturers
- Whisper STT accuracy in noisy, real-world environments (tested cleanly indoors)
- Camera OCR latency on mid-range hardware under real lighting

These are listed here instead of left implicit, because a demo that only ever ran in a quiet room with a flagship device is a different claim than "this works."

## Building

```bash
git clone <repo-url>
cd arthix
./gradlew assembleDebug
```

Minimum SDK 26. No API keys, no `.env`, no backend to stand up — it's a single Android module and it runs the moment it builds.

## Architecture at a glance

```
com.chirag.arthix
├── data        Room entities, DAOs, SQLCipher setup
├── domain      Reconciliation engine, deterministic math, repositories
├── sensor      Accelerometer oscillation detection, grace-window service
├── notification NotificationListenerService, bank/UPI parsing
├── ocr         CameraX + ML Kit receipt scanning
├── voice       Whisper STT, phonetic name matching, split-by-voice
└── report      Paise-precision computation + grounded LLM phrasing
```

Dependency injection is wired end-to-end with Hilt: DAOs → Repositories → ViewModels, no manual construction anywhere in the graph.

---

Built for the FinTech & Commerce track, iQOO Hackathon 2026 — Pune City Battle.

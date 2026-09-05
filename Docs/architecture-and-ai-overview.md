# ARTHIX — Architecture & AI Overview
> *A simple, plain-English guide to how ARTHIX works under the hood, how we use on-device AI/models, and why we built it this way.*

---

## 1. What is ARTHIX in 30 Seconds?

**ARTHIX** is an intelligent, privacy-first personal finance manager for Android built specifically for India's UPI payments ecosystem. 

Unlike traditional expense trackers that require tedious manual entry:
1. **It detects payments automatically** when you pay with Google Pay, PhonePe, or Paytm.
2. **It lets you log expenses by voice** (e.g. *"split ₹500 between Parikshit and Chiru"*).
3. **It splits bills with friends** and can automatically send silent SMS debt reminders to your close friends.
4. **It plans your savings goals** using on-device intelligence.
5. **Everything stays 100% on your phone** — zero cloud servers, zero trackers, zero data leaks.

---

## 2. High-Level Architecture (The Big Picture)

ARTHIX is divided into 3 clean layers: **Input**, **Brain (Intelligence)**, and **Action/Storage**.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           1. INPUT / SENSING                            │
│  • Bank / UPI Notifications (NotificationListenerService)                │
│  • Voice Microphone (SpeechRecognizer & Whisper AI)                     │
│  • Accelerometer / Gyroscope (Double-shake gestures)                    │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    2. INTELLIGENCE & PROCESSING (BRAIN)                 │
│  • OpenAI Whisper Tiny ONNX (Offline Speech-to-Text)                    │
│  • VoiceIntentParser & Indian Phonetic Normalizer ("neeru" -> "Niru")   │
│  • AiGoalPlannerEngine (Cash flow feasibility & safety buffers)         │
│  • GroundingValidator (Anti-hallucination math whitelist)               │
│  • Pure Integer Paise Math Engine (Zero floating-point rounding bugs)   │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       3. ACTION & ENCRYPTED STORAGE                     │
│  • SQLCipher + Room (AES-256 encrypted local database)                  │
│  • Floating Overlay Bubble (SYSTEM_ALERT_WINDOW over payment apps)      │
│  • Silent SMS Dispatch (Android SmsManager for Close Friends debt)      │
│  • Modern Jetpack Compose UI (Smooth cylinder sliders & dark theme)     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. How We Use the "Stuffs" (Core Components)

### A. The Floating Category Bubble (`SYSTEM_ALERT_WINDOW`)
- **What it does:** The second you finish paying at a tea stall or grocery store with UPI, a tiny floating bubble pops up directly on top of your payment app.
- **Why it matters:** You can tap "Food" or "Groceries" with a single thumb tap in under 2 seconds. You don't have to unlock your phone later, open an app, and try to remember what you spent ₹40 on.

### B. Smart Shake Detection (`CaptureGraceWindowService`)
- **What it does:** You can shake your phone twice to dismiss or trigger quick categorization.
- **The clever trick:** Running motion sensors 24/7 drains the phone battery in a few hours. ARTHIX only powers on the accelerometer for a **10-second grace window** right after a payment app is closed. Zero battery drain during normal use.

### C. Encrypted Local Storage (`SQLCipher` + `Room`)
- **What it does:** Stores all transactions, savings goals, split records, and close friends locally.
- **Security:** Uses 256-bit AES encryption. Even if someone physically clones your phone's memory or roots your device, the database looks like completely scrambled noise without the hardware key stored in Android Keystore.

### D. Close Friends & Silent SMS Reminders
- **What it does:** Keeps a small local list of 3–5 friends you regularly split bills with (like roommates or lunch buddies).
- **Automation:** When you split a ₹1,200 dinner, ARTHIX uses Android's native `SmsManager` to send a clean reminder directly in the background (e.g. *"Hi Parikshit, your share for Dinner is ₹400. Settle via UPI to 9876543210"*). You don't have to manually switch between WhatsApp or Messages 4 times.

### E. Pure Integer Paise Math (No Floating Numbers)
- **What it does:** Every financial number in ARTHIX is handled as a 64-bit integer (`Long`) in **paise** (₹1 = 100 paise).
- **Why it matters:** Computers make rounding mistakes when doing division with decimals (e.g. `₹50.0 / 3` becomes `₹16.666666666666668` or `₹49.9999999`). In ARTHIX, the math is exact down to the last paisa, and balances always sum up to 100% cleanly.

---

## 4. How We Use AI / LLM (And Why We Don't Use Cloud LLMs)

### The Golden Rule: Financial Privacy First
Most modern apps send your bank statements and voice recordings to remote cloud APIs (like OpenAI ChatGPT, Anthropic Claude, or cloud servers). 

**In ARTHIX, we explicitly rejected cloud LLMs.**
- Your financial logs and bank SMS never leave your phone.
- The app works completely offline in remote locations or basements without an internet connection.
- Cloud API calls add 1.5 to 3 seconds of network delay. ARTHIX responds in under 50 milliseconds.

---

### What AI & Machine Learning Do We Actually Use?

#### 1. Quantized OpenAI Whisper Tiny (ONNX Runtime)
- **Role:** On-Device Speech-to-Text (STT).
- **How it works:** We packaged a lightweight, quantized int8 ONNX model (~39MB) that runs directly on your phone's CPU via Microsoft's ONNX Runtime.
- **Why:** When you speak fast Indian English (e.g., *"split four-fifty with Aman and Priya"*), standard speech engines often stumble. Whisper runs locally, works offline, and accurately transcribes accented speech.

#### 2. Indian Phonetic Normalization (`normalizePhonetic`)
- **Role:** Accent and spelling tolerance.
- **How it works:** Speech-to-text engines often transcribe Indian names phonetically:
  - Spoken *"Niru"* $\rightarrow$ Transcribed as *"Neeru"*
  - Spoken *"Puja"* $\rightarrow$ Transcribed as *"Pooja"*
  - Spoken *"Aman"* $\rightarrow$ Transcribed as *"Amman"*
- **Our Engine:** Normalizes vowel and consonant variations (`ee` $\leftrightarrow$ `i`, `oo` $\leftrightarrow$ `u`, `w` $\leftrightarrow$ `v`, `ph` $\leftrightarrow$ `f`) and duplicate letters. If you speak *"split with neeru"*, ARTHIX instantly knows you mean your saved friend **Niru** without asking you to create manual nicknames.

#### 3. Voice Intent Parser (`VoiceIntentParser`)
- **Role:** Natural language understanding.
- **How it works:** A deterministic NLP parser extracts amounts, merchant names, split candidates, and categories from transcripts like *"spent 350 at Starbucks"* or *"split ₹1200 between Parikshit and Chiru"*.

#### 4. Deterministic AI Goal Planner (`AiGoalPlannerEngine`)
- **Role:** Financial target feasibility & recommendation.
- **How it works:** Rather than letting a generative LLM make up arbitrary numbers, this engine analyzes:
  - Monthly net income
  - Fixed non-discretionary expenses
  - Free discretionary cash flow
  - Minimum emergency buffer (20% of monthly free cash flow)
- It calculates realistic feasibility dates, tells you if a goal is too aggressive, and computes the exact monthly savings required without any mathematical hallucination.

#### 5. Anti-Hallucination Guardrails (`GroundingValidator`)
- **Role:** Numeric integrity.
- **How it works:** A strict validator ensures that any numeric figure or recommendation presented to the user matches mathematically verified figures from the database. A language model is never allowed to "invent" a transaction amount or bank balance.

---

## 5. Summary Comparison

| Requirement | Traditional Cloud Approach | ARTHIX On-Device Architecture |
| :--- | :--- | :--- |
| **Privacy** | Data sent to third-party cloud servers | **100% on-device**; data never touches internet |
| **Connectivity** | Requires 4G/5G/Wi-Fi to function | **Works offline** in flights, metros, or basements |
| **Speed** | 1,500ms – 3,000ms latency per request | **< 50ms** instant local execution |
| **Accuracy** | LLMs can hallucinate numeric figures | **Mathematically grounded** integer arithmetic |
| **Cost** | Recurring API costs per API token | **Zero server costs**, runs on user's hardware |

---

## 6. Key Files in the Repository

If you want to explore the code:
- **Speech & AI Intent:** [`app/.../voice/VoiceIntentParser.kt`](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/voice/VoiceIntentParser.kt)
- **Indian Phonetic Matcher:** [`app/.../domain/split/CloseFriendMatcher.kt`](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/domain/split/CloseFriendMatcher.kt)
- **AI Goal Planner Engine:** [`app/.../ai/AiGoalPlannerEngine.kt`](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/ai/AiGoalPlannerEngine.kt)
- **Split Screen & UI Sliders:** [`app/.../ui/screen/split/SplitBillScreen.kt`](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/ui/screen/split/SplitBillScreen.kt)
- **Encrypted Database:** [`app/.../data/database/AppDatabase.kt`](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/data/database/AppDatabase.kt)
- **Engineering Diary:** [`EngineeringDiary/diary.md`](file:///c:/Users/Niru/Documents/coding/ARTHIX/EngineeringDiary/diary.md)

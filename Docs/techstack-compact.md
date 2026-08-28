# Shake & Audit — Technical Stack & Architecture (compact)

Companion to project-brief.md and edge-case-analysis.md. Locks down tech choices across 3 judged modules: AI/ML/Agentic, UI/Backend, Sensor Capture. Every choice ties to a brief constraint or closes a specific edge case (ECxx).

## 0. Two governing decisions
- **Native Android (Kotlin), not cross-platform.** Core value lives in OS APIs (NotificationListenerService, SensorManager, foreground services, heads-up action buttons, Doze exemptions) that Flutter/RN fight rather than expose. iOS out of scope (§2.5) → no cross-platform benefit. Kotlin, native SDK, min SDK 26 (Oreo, for notification channels).
- **Deterministic core, generative edges.** Anything touching money/matching = plain testable Kotlin, no model in the loop. LLM only phrases already-verified numbers and turns ambiguous audio/image into structured intent. Implements EC-48; keeps the "Agentic" module intentionally narrow.

## 1. Sensor Module (Capture Layer) — FR-1, FR-2, FR-2a
Highest-risk, most-novel part per edge-case doc (most Critical items land here).

| Concern | Choice | Implements |
|---|---|---|
| Shake detection | SensorManager + TYPE_LINEAR_ACCELERATION; SENSOR_DELAY_GAME screen-on / SENSOR_DELAY_NORMAL screen-off | EC-05 |
| Real shake vs false positive | Oscillation detector: ≥2 direction reversals above threshold in window, not single spike | EC-01 |
| Shake vs shake-and-hold | One state machine (Idle→Shaking→HoldConfirmed); stop-before-hold=commit FR-1, continue-past-hold=cancel FR-1 side effects, discard in-flight PendingCapture, switch to FR-7 | EC-03 |
| Chip UI | Heads-up notification + inline Notification.Action buttons, not SYSTEM_ALERT_WINDOW | EC-04 |
| Background survival | Foreground Service + persistent low-priority notification + battery-optimization whitelist prompt in onboarding | EC-59 (mitigation only), EC-05 |
| Notification capture | NotificationListenerService; package-name allow-list (GPay/PhonePe/Paytm pkg IDs) via StatusBarNotification.getPackageName() checked **before** any text parsing | EC-08, EC-56 |
| Notification text extraction | Read EXTRA_BIG_TEXT/EXTRA_TEXT_LINES first, fallback EXTRA_TEXT; truncated/mid-word matches = low-confidence | EC-10 |
| Pattern matching | Regex set in editable JSON config, not hardcoded; outcome-keyword filter (declined/failed/pending/reversed) before treating as completed outflow | EC-07; mitigates EC-09 |
| Amount normalization | One shared AmountParser for ₹/Rs./INR + separator variants | EC-11 |
| Grouped notifications | Inspect InboxStyle/child entries in summary notifications, don't assume 1 notif = 1 payment | EC-12 |
| Dedup (app+bank notif) | Before committing, check existing txn w/ same amount + close payee match within a few seconds | EC-06 |

### 1.1 Reconciliation Engine (FR-2a) — highest-leverage code in the project
Rule-based, not ML — deterministic & testable, scores well on "Technical depth."
- **Concurrency:** shake events (SensorEventListener thread) + notification events (binder thread) both post to a single serial CoroutineDispatcher, the only writer to the pending queues. Hard requirement — fixes a real race condition. → EC-18
- **Clock:** SystemClock.elapsedRealtime() (monotonic) for all matching math, never currentTimeMillis(). → EC-19
- **Matching:** nearest-neighbor by time-delta within 2-min window (already in brief), not FIFO.
- **Ambiguity threshold:** score-gap rule — compare best vs 2nd-best candidate time-delta; gap below an empirically-tuned (Phase 5 data) threshold → ambiguous → one-tap disambiguation prompt. → EC-14
- **Disambiguation timeout:** ignored prompt → auto-pick best guess after short timeout, flag record `auto_resolved` for review; never wait indefinitely or default silently. → EC-15
- **Timeout hygiene:** on timeout, remove entry from active matching pool at the same instant txn moves to `awaiting_*` status. → EC-17
- **Discard path:** first-class "not a transaction" discard action from both chip UI and voice — single most important addition in the whole risk doc. → EC-16, EC-21, EC-25

### 1.2 Camera OCR (FR-4)
- Capture: CameraX. OCR: ML Kit Text Recognition v2 (on-device, offline, satisfies NFR-1); weak on handwritten/faded receipts (EC-31, accepted limitation).
- Amount resolution: keyword-proximity ("Total"/"Grand Total"/"Amount Payable") first, fallback = largest number, flagged low-confidence. → EC-30
- Sanity bounds: reject/flag amounts outside plausible range (<₹1 or >₹50,000) for manual confirm. → EC-32
- Latency target: a few seconds, tested on demo device (outside NFR-4's report-only 15s budget). → EC-34

## 2. AI / ML / Agentic Module
Intentionally smaller than typical "AI hackathon" scope (§0). Three on-device, offline-capable components:

| Component | Tech | Role |
|---|---|---|
| Speech-to-text | Vosk (small Indian-English model) | FR-3 voice follow-up, FR-6 voice split |
| OCR | ML Kit Text Recognition v2 | FR-4 (see §1.2) |
| Report phrasing | On-device quantized LLM, MediaPipe LLM Inference API (Gemma 3 1B / Gemma 2 2B, int4/int8) | FR-7 — phrasing only, never arithmetic |

**Vosk over SpeechRecognizer:** built-in offline mode is device/OEM-dependent (compounds EC-59); Vosk is offline by construction, small footprint, exposes confidence scores needed for:
- Confidence-gated re-prompt: below-threshold → re-prompt once → fallback to tap chip. → EC-27
- Discard-intent recognition ("skip"/"not real"/"ignore that one"). → EC-25
- Amount resolution in voice flow, not just category — biggest current hole (unmatched shakes stuck incomplete otherwise). → EC-24
- Free-text category fuzzy-matched to nearest existing category, raw phrase kept as sub-tag. → EC-28
- Explicit Hindi-English code-switch language scoping/testing before Phase 6. → EC-29

**Why an on-device LLM at all:** direct answer to "creative phone use" rubric (15%) and "personalized agent" business goal. Trade-off (per EC-48 fundamental limit): free-generated phrasing always carries some residual wording risk even with grounded numbers; full rigid templating removes risk but kills the "agent" feel. Mitigation, not elimination:
1. Compute all numerics (sums, deltas, projections) in Kotlin first, grounded in real data (NFR-5).
2. Pass only verified numbers into the LLM prompt; instruct it to phrase only, never invent/restate different figures.
3. Post-validate output — every number in generated text must match the whitelist passed in; fail → fallback to template sentence.
4. Anchor projections to Phase 6 pre-seeded historical baseline, not raw live-session rate (2-txn/2-min sample can extrapolate to absurd numbers). → EC-43
5. Explicit "no prior data" branch instead of divide-by-zero on week-over-week %. → EC-45
6. Include pending/unlabeled amounts in overall total (never invisible); exclude only from category suggestions until resolved. → EC-44

**Latency:** grounding approach also satisfies NFR-4's 15s budget — model phrases only, doesn't compute. → EC-49. Use smallest quantized model that stays coherent; benchmark on actual demo device early.

**Reconciliation & split-group suggestion: deliberately not ML.** Rule-based nearest-neighbor w/ defined ambiguity threshold (§1.1); split suggestion = category-match + time-of-day/day-of-week heuristic w/ explicit cold-start default (no suggestion, prompt manually, no guessing). → EC-41. Deterministic = debuggable = what "Technical depth" rewards.

### 2.1 Office Kit bridge — enhancement layer, never a dependency
Optional Green Light compute path for FR-7 report gen (iQOO/vivo-specific infra; pull real SDK docs later; keep behind a pluggable interface). Brief's Constraint 1.4 (no laptop dependency) + Success Criterion 1.5 (live on-device demo) make the on-device path the mandatory default. Bridge, when connected, runs a bigger model / offloads compute — earns the separate 10% "Office Kit usage" rubric line — but is additive only. → EC-47; avoids the classic hackathon failure of a bridge that won't pair on stage.

## 3. UI / Backend Module

| Concern | Choice | Why |
|---|---|---|
| UI framework | Jetpack Compose | Fastest path to chip UI, report screen, split-confirm screen, edit/history screen |
| Architecture | MVVM + Repository, mapped onto brief's 4 subsystems (Capture/Storage/Agent/UI) | Keeps reconciliation engine & report agent testable in isolation; supports live edge-case demoing (EC-62) |
| Local persistence | Room (SQLite), WAL journal mode | Atomic writes — crash mid-write on a money record unacceptable → EC-54 |
| Concurrency/event bus | Kotlin Coroutines + Flow/Channel, single serial dispatcher for reconciliation (§1.1) | Correctness-first |
| Background scheduling | WorkManager for idle-detection polling + voice-followup triggers | Survives process death better than raw Handler; Doze-aware |
| Data model changes | See §3.1 | Closes several critical/high gaps at once |

### 3.1 Data model deltas from brief §2.4
```
Transaction {
  id, amount_paise: Long, payee, category, timestamp,   // integer paise (EC-46)
  direction: [inflow | outflow],
  source: [shake | voice | camera | manual],
  status: [confirmed | awaiting_match | awaiting_category |
           awaiting_amount | discarded],                 // replaces pending: bool (EC-53)
  source_capture_id: String?,                             // traceability (EC-23)
  source_notification_id: String?,
  confidence_flag: [clean | auto_resolved | needs_review] // EC-15, EC-22, EC-30, EC-32
}
PendingCapture { id, timestamp_monotonic, matched: bool, category? }      // now persisted (EC-51)
PendingNotification { id, timestamp_monotonic, amount_paise, payee, matched: bool }  // now persisted
SplitRecord {
  transaction_id, participants[], share_per_person_paise,
  confirmed_via: [tap | voice],
  amount_lock: [live | locked_at_creation]   // explicit rule (EC-40); recommend "live", document choice
}
Report {
  period_start, period_end, category_breakdown{},
  net_flow, suggestions[], projected_total, projected_savings,
  uncategorized_total_paise   // pending amounts stay visible (EC-44)
}
```

### 3.2 New screens/flows (not in original FR list)
- Edit/delete transaction screen — highest-priority addition, no capture path currently correctable. → EC-52
- Manual fallback (FR-5) as persistent quick-access affordance, not menu-buried — underpins every other failure path. → EC-35
- Onboarding screen explaining notification-listener permission before the system's own alarming dialog. → EC-58

### 3.3 Security & privacy baseline
- Package allow-list enforced at NotificationListenerService entry; non-allow-listed content never logged/cached/retained, even transiently. → EC-56
- At-rest encryption (SQLCipher/Keystore) if time allows; else a consciously-flagged gap, not a silent oversight. → EC-57
- No cloud sync anywhere — explicit accepted boundary per §2.5. → EC-55

## 4. Rubric mapping
| Rubric line | Wt | What earns it |
|---|---|---|
| End product quality | 30% | Deterministic core + edit/delete + discard paths + graceful degradation = survives real use |
| Novelty and impact | 20% | Reconciliation engine (nearest-neighbor, per-capture timeout, disambiguation) is the actual novel contribution |
| Creative phone use | 15% | Camera (ML Kit), voice (Vosk, bidirectional), on-device LLM (MediaPipe/Gemma) — all offline, genuinely used |
| Technical depth | 15% | Single-threaded event processing fixing a real race condition, monotonic clock, grounded-LLM anti-hallucination pattern |
| Office Kit usage | 10% | Bridge as additive enhancement behind a pluggable interface — points without risking the core demo |
| Demo and presentation | 10% | Live-triggering the disambiguation prompt (EC-62) beats describing it verbally |

## 5. Build order
Build reconciliation engine (§1.1) + notification allow-list/parser (§1) **first** — fully persistent and thread-safe before anything else, since most other modules depend on or become safer once these are solid. LLM phrasing layer (§2) goes in last, since its correctness depends on everything upstream already being trustworthy.

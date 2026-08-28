# Arthix (Shake & Audit) — Master Build Plan & Per-Phase PRD Generator Prompts

**Purpose of this document:** This is not the PRD itself. This is a set of ready-to-paste
prompts. Each prompt, when given to a Claude agent **along with all three source files**
(`shake-audit-project-brief.md`, `edge-case-analysis.md`, `techstack.md`), generates a
**standalone, industry-grade, phase-specific PRD** — detailed enough that the assigned
teammate could build entirely from it without re-reading the other two docs.

**How to use this doc:**
1. Go phase by phase, in order. Do not start Phase N+1's PRD generation until Phase N's
   PRD exists (later phases explicitly build on artifacts/interfaces from earlier ones).
2. For each phase, open a fresh Claude conversation, upload all 3 original files, and
   paste that phase's full prompt block.
3. The generated PRD is owned by one person only. That person builds it standalone.
4. Cross-phase interfaces (data model, IDs, status enums) are locked in Phase 0 —
   every later prompt is told to treat Phase 0's output as a frozen contract, not a
   suggestion.

---

## Why 7 phases, not the brief's original 6 — and why this ownership split

The original brief's Phase 1 ("Capture Layer") bundles three things that belong to
**two different people** per your team split: Parikshit owns raw sensor/notification
capture, but the reconciliation engine and local persistence are really backend/data
plumbing that Chirag's UI/backend module needs to own directly, since Room, the data
model, and the repository layer live in his module per `techstack.md` §3. Splitting
naively by the brief's phase numbers would hand Parikshit a phase he can't finish alone
(he'd be blocked on Chirag's Room schema) and hand Chirag a phase with no clear start
point (he'd be blocked on Parikshit's raw events).

So this plan uses **7 phases**, reordered around who can start immediately vs. who is
downstream, matching `techstack.md` §5's stated build-order priority ("reconciliation
engine and notification allow-list/parser first, fully persistent and thread-safe,
before anything else"):

| # | Phase | Owner | Depends on |
|---|---|---|---|
| 0 | Data Model, Schema & Cross-Module Contracts | **Chirag** | Nothing — build first |
| 1 | Sensor Capture — Shake Detection & Gesture State Machine | **Parikshit** | Phase 0 (schema only, not logic) |
| 2 | Notification Capture & Reconciliation Engine | **Parikshit** | Phase 0, Phase 1 |
| 3 | UI Layer, Persistence Wiring & Manual/Edit Flows | **Chirag** | Phase 0, Phase 2 |
| 4 | Voice Capture, OCR & Intent Recognition | **Niranjan** | Phase 0, Phase 3 (chip UI to pre-fill into) |
| 5 | Agentic Report Layer (Categorization, Projections, LLM Phrasing) | **Niranjan** | Phase 0, Phase 3 |
| 6 | Split Feature (Splitwise-style) | **Chirag** | Phase 0, Phase 3, Phase 4 (voice split) |
| 7 | Integration, Test Pass & Demo Readiness | **All three, jointly** | Everything above |

Rationale for each reassignment vs. the brief's raw FR grouping:
- **Phase 0 is new** and deliberately owned by Chirag alone, ahead of everyone else,
  because `EC-51`, `EC-52`, `EC-53`, `EC-23` all say the *same* thing: the original
  data model in the brief is underspecified, and every other phase's edge cases
  (traceability, status enums, confidence flags) only close if the schema is right
  **before** Parikshit and Niranjan start writing against it. Building this once,
  first, and freezing it avoids three people inventing three incompatible schemas.
- **Phase 1 and 2 are split** (sensor detection vs. reconciliation+notifications)
  even though the brief treats them as one Phase 1, because EC-18's race condition
  fix requires the reconciliation engine to already know both event sources' shapes
  — Parikshit should finish and unit-test raw shake detection in isolation (Phase 1)
  before wiring it into the two-queue engine (Phase 2), so bugs in gesture detection
  don't get confused with bugs in matching logic.
- **OCR moves from "Phase 2: Secondary Capture" into Niranjan's module (Phase 4)**
  alongside voice, not Parikshit's, because ML Kit OCR and Vosk STT are both AI/ML
  components per `techstack.md` §2's own module breakdown ("OCR ... listed here for
  completeness" under the AI/ML module) — Parikshit's module is sensors (accelerometer,
  notification listener), not perception models.
- **The Agent/Report layer is its own phase (5)**, separated from voice/OCR (4),
  because it has a fundamentally different risk profile (LLM grounding, EC-48) and
  a different, later dependency (needs real transaction history to report over,
  which voice/OCR help produce) — bundling them would force Niranjan to context-switch
  between "make STT robust to noise" and "make an LLM never hallucinate a number"
  inside one PRD.
- **Split (FR-6) is pulled out into its own phase (6)**, owned by Chirag, instead of
  living inside the agent layer or the UI phase, because it's UI+data logic
  (group suggestion heuristic, rounding rule, tap-to-confirm) with only a thin
  voice-intent dependency on Niranjan's Phase 4 output — bundling it into Phase 3
  would make that phase too large to be "standalone," and bundling it into
  Niranjan's phases would hand him UI/data-model work outside his module.
- **Phase 7 (Test + Demo Readiness) is explicitly joint**, matching the brief's own
  Phase 5/6 structure and `EC-61`/`EC-62`, because reliability and demo-readiness
  testing cannot be done by one person against their own code in isolation — the
  brief's own exit criteria (5 consecutive full-flow runs, concurrent-payment stress
  tests) require all three subsystems running together.

---

## Locked cross-phase contract (read this before using any prompt below)

Every phase prompt below instructs the generating agent to treat the following as
**frozen** — sourced directly from `techstack.md` §3.1 and the edge-case punch list —
and to design that phase's PRD to be compatible with it, not to reinvent it:

- `Transaction.status` is the enum `confirmed | awaiting_match | awaiting_category |
  awaiting_amount | discarded` — never a boolean.
- `Transaction.amount_paise` is an integer (paise), never a float.
- `Transaction.confidence_flag` is `clean | auto_resolved | needs_review`.
- `Transaction.source_capture_id` / `source_notification_id` provide traceability.
- `PendingCapture` and `PendingNotification` are **persisted** (Room), not in-memory.
- All internal time-delta matching math uses `SystemClock.elapsedRealtime()`
  (monotonic), never wall-clock time.
- Every automated capture path needs a "not a transaction" / discard action, reachable
  from both tap and voice surfaces.
- Every automated capture path needs an edit/delete affordance downstream (Phase 3).
- The reconciliation engine's ambiguity check is a **numeric score-gap rule**, not a
  qualitative judgment call.
- LLM usage anywhere in the system is phrasing-only; all numbers reaching the model are
  pre-computed and verified in Kotlin first, and model output is validated against that
  whitelist before rendering, per `techstack.md` §0.2 and §2.

---

# PHASE 0 — Data Model, Schema & Cross-Module Contracts
**Owner: Chirag · Depends on: nothing, build first**

## Prompt to paste (with all 3 files attached)

```
You are generating a standalone, industry-grade Phase PRD for a hackathon project
called Arthix ("Shake & Audit"). I am attaching three files for full context:
shake-audit-project-brief.md (the BRD/PRD), edge-case-analysis.md (the risk/edge-case
pass), and techstack.md (the locked tech stack and architecture decisions). Read all
three fully before writing anything — this phase's PRD must be internally consistent
with the constraints, edge cases, and tech choices already decided in those documents,
not a reinvention of them.

CONTEXT: This is Phase 0 of a 7-phase build plan, split across a 3-person team
(Chirag: UI/backend, Parikshit: sensors/input, Niranjan: AI/ML/OCR/voice/agentic).
This phase is owned SOLELY by Chirag and must be finished and frozen before any other
phase's code is written, because Phases 1 through 6 all write against the schema this
phase defines.

YOUR TASK: Write a complete, standalone PRD for "Phase 0 — Data Model, Schema &
Cross-Module Contracts" that a single developer (Chirag) could execute from start to
finish with zero ambiguity, with no need to reference the original brief or edge-case
doc again. Cover:

1. SCOPE: This phase produces ONLY the Room database schema, entity classes, DAOs,
   the repository interface layer (not implementations that depend on unbuilt
   modules), and a written data contract document — no UI, no sensor code, no AI code.

2. ENTITY-BY-ENTITY SPEC: For each of Transaction, PendingCapture, PendingNotification,
   SplitRecord, and Report — full Kotlin data class / Room @Entity definition, every
   field with its exact type (use amount_paise: Long, never Float/Double — ground this
   in EC-46), every enum with every possible value spelled out (status enum per EC-53:
   confirmed | awaiting_match | awaiting_category | awaiting_amount | discarded;
   confidence_flag per EC-15/EC-22/EC-30/EC-32: clean | auto_resolved | needs_review),
   nullability rules, and foreign-key relationships (source_capture_id /
   source_notification_id per EC-23 for traceability). Explicitly persist
   PendingCapture and PendingNotification as Room entities, not in-memory structures
   (EC-51) — specify their table definitions with the same rigor as Transaction.

3. INDICES AND QUERY PATTERNS: Specify which fields need DB indices given the query
   patterns later phases will need (e.g., reconciliation engine will query
   PendingCaptures/PendingNotifications by timestamp_monotonic range and matched
   status very frequently — index accordingly; report generation will query
   Transactions by timestamp range and category).

4. DAO INTERFACES: Full method signatures for each DAO (insert, update, delete, and
   the specific range/filter queries each downstream phase will need — anticipate
   Phase 2's need to query unmatched pending entries within a time window, Phase 3's
   need for a full transaction history list ordered by timestamp, Phase 5's need for
   category-grouped sums within a date range).

5. WAL MODE & ATOMICITY: Specify Room database builder configuration with WAL journal
   mode explicitly enabled, and document why (EC-54 — a crash mid-write on a money
   record is unacceptable, use the actual RoomDatabase.Builder call needed).

6. REPOSITORY LAYER CONTRACT: Define the repository interfaces (TransactionRepository,
   PendingQueueRepository, SplitRepository, ReportRepository) that Phases 1-6 will
   code against, as pure interfaces/abstractions — implementation bodies belong to
   later phases, but the interface signatures must be final here so no later phase
   has to modify them.

7. AMOUNT-HANDLING UTILITY: Specify the shared AmountParser/amount-normalization
   utility class contract (EC-11) that Phase 2's notification parsing and Phase 4's
   OCR extraction will both call — normalizes ₹/Rs./INR symbol variants and
   thousands separators into amount_paise: Long. Define its exact function signature
   and behavior on malformed input here, even though its callers are built later.

8. MIGRATION STRATEGY: Since this is a hackathon build with a fixed demo date, specify
   a simple but real Room migration policy (e.g., destructive migration acceptable
   pre-demo, but document the exact fallbackToDestructiveMigration() call and why it's
   a conscious choice, not an oversight — mirror the tone of EC-55/EC-57's "accepted
   limitation, stated plainly" pattern from the edge-case doc).

9. STEP-BY-STEP BUILD SEQUENCE: Ordered, numbered implementation steps from empty
   project to a fully compiling, unit-tested persistence layer with no UI — include
   what to build first, what depends on what, and a suggested time allocation given
   this is one phase of a multi-day hackathon.

10. UNIT TEST PLAN: Concrete test cases for this phase specifically — CRUD roundtrips
    for every entity, index performance sanity checks, WAL crash-recovery simulation,
    AmountParser edge cases (₹1,450.00 vs Rs.1450 vs INR 1450.50, malformed strings).

11. EXIT CRITERIA: A precise, checkable list of what "Phase 0 done" means, in the same
    style as the original brief's Phase 1-6 exit criteria — e.g., "all five entities
    compile and pass Room's schema export/validation," "a hand-written unit test can
    insert a Transaction, a PendingCapture, and a PendingNotification and query them
    back with correct field values," "AmountParser correctly normalizes all sample
    formats in section 10 into amount_paise."

12. HANDOFF NOTES: A short, explicit section titled "What Phases 1-6 can assume is
    true after this phase" and "What Phases 1-6 must NOT change without renegotiating
    with Chirag" — since every other phase's PRD will be generated separately and
    told to treat this phase's schema as frozen.

Format the output as a complete, professional PRD document with numbered sections,
suitable for a solo engineer to execute against without further clarification. Do not
summarize the brief or edge-case doc back at me — synthesize them into concrete,
buildable specification. Cover every edge case from edge-case-analysis.md that is
relevant to data modeling and persistence (Section I in that doc, plus EC-11, EC-23,
EC-40, EC-46) explicitly, by ID, with the fix baked into the spec rather than
mentioned as a caveat.
```

---

# PHASE 1 — Sensor Capture: Shake Detection & Gesture State Machine
**Owner: Parikshit · Depends on: Phase 0 (schema, read-only)**

## Prompt to paste (with all 3 files attached)

```
You are generating a standalone, industry-grade Phase PRD for a hackathon project
called Arthix ("Shake & Audit"). I am attaching three files for full context:
shake-audit-project-brief.md, edge-case-analysis.md, and techstack.md. Read all three
fully before writing anything.

CONTEXT: This is Phase 1 of a 7-phase build plan (3-person team: Chirag owns UI/
backend/data, Parikshit owns sensors/input, Niranjan owns AI/ML/OCR/voice/agentic).
This phase is owned SOLELY by Parikshit. Phase 0 (data model, owned by Chirag) is
already complete and FROZEN — this phase writes new event-detection code that will
later feed into Phase 0's PendingCapture entity, but does not modify Chirag's schema.
Treat the following as a fixed, non-negotiable contract from Phase 0: PendingCapture
is a persisted Room entity with fields id, timestamp_monotonic, matched: bool,
category (nullable). Do not redesign it — build against it.

YOUR TASK: Write a complete, standalone PRD for "Phase 1 — Sensor Capture: Shake
Detection & Gesture State Machine" (implements FR-1 and the gesture-detection half of
FR-7's shake-and-hold trigger) that Parikshit could execute start to finish with zero
ambiguity. Cover:

1. SCOPE: Raw accelerometer/sensor event handling and gesture classification ONLY.
   This phase does NOT touch notification listening, the reconciliation engine's
   matching logic, or persistence writes beyond emitting a well-defined event/callback
   that Phase 2 will consume. Explicitly define the emitted event contract (what
   Parikshit's code hands off, and to whom) as this phase's primary deliverable.

2. SENSOR CONFIGURATION: Exact SensorManager setup — TYPE_LINEAR_ACCELERATION (not
   raw accelerometer, and explain why per techstack.md), SENSOR_DELAY_GAME while
   screen-on, SENSOR_DELAY_NORMAL while screen-off, and the screen-state listener
   needed to switch between them. Cite EC-05 (battery/Doze impact) explicitly and
   specify the foreground Service + persistent low-priority notification requirement
   that mitigates it, plus the battery-optimization whitelist onboarding prompt
   (also mitigates EC-59).

3. OSCILLATION-BASED SHAKE DETECTOR: Full algorithm specification — not just "detect
   a shake" but the actual state machine: require at least 2 direction reversals
   above a tunable acceleration threshold within a tunable detection time window.
   Give concrete starting parameter values to tune from (state that these need
   empirical tuning against real device testing, and specify the tuning test
   protocol: walking, pocket-jostling, vehicle vibration as three DISTINCT negative
   test cases per EC-01, not lumped as one "handling" bucket). Also address EC-02
   (false negatives from gentle/rushed shakes) explicitly — state the precision/
   recall tradeoff as a named, accepted design constraint (per the edge-case doc's
   "Fundamental Limits" section) rather than something this phase is expected to
   fully solve, and specify that this phase's job is to make the threshold tunable
   via a config value, not hardcoded, so it can be adjusted after Phase 5 testing
   without a code change.

4. DEBOUNCE LOGIC: Exact 2-second cooldown window implementation after a detected
   shake, to prevent one physical shake from producing two PendingCapture events —
   specify precisely how the debounce timer resets and interacts with the oscillation
   detector's own internal window.

5. SHAKE VS. SHAKE-AND-HOLD STATE MACHINE: This is the most technically important
   part of this phase. Specify the full state machine (Idle → Shaking → HoldConfirmed)
   as described in techstack.md §1: on motion-stop before the hold-duration threshold
   → commit as a Phase-1 shake event; on motion continuing past the hold threshold →
   explicitly cancel any in-flight Phase-1 side effects (this phase must emit a
   cancellation/discard signal, not just silently switch behavior) and emit a
   shake-and-hold event instead for Phase 5's report trigger to consume. Ground this
   directly in EC-03 and give the exact hold-duration threshold to start tuning from.
   Define both events' payload shape precisely (what data crosses the module boundary).

6. HEADS-UP NOTIFICATION CHIP UI TRIGGER: Specify exactly how this phase triggers the
   category-selection chip (Notification.Action inline buttons on a heads-up
   notification, NOT SYSTEM_ALERT_WINDOW — ground this in EC-04 and explain why the
   overlay-permission approach was rejected). Note that the chip's own UI rendering
   is technically Chirag's module (Phase 3) — this phase's job is to fire the trigger
   with the right category options and auto-dismiss timer (2s), and specify the exact
   interface Parikshit's code calls to do so, decided as a contract with Phase 3.

7. FOREGROUND SERVICE & LIFECYCLE: Full specification of the foreground Service
   implementation — manifest declaration, notification channel setup (min SDK 26
   requirement per techstack.md §0.1), start/stop lifecycle tied to app state, and
   explicit handling for OriginOS's aggressive background-kill behavior (EC-59):
   specify the battery-optimization whitelist request flow, and specify precisely
   what "graceful degradation" means here — i.e., what Parikshit's code should do
   (or signal) if the OS has killed the service and it restarts, so that a silent
   miss becomes visible/traceable rather than invisible.

8. CONCURRENCY HANDOFF TO PHASE 2: Specify precisely how shake events get posted to
   the single serial CoroutineDispatcher that Phase 2 owns (EC-18) — this phase does
   not implement the dispatcher itself (that's Phase 2/reconciliation engine's job)
   but must post events onto it correctly, using SystemClock.elapsedRealtime() for
   all timestamps (EC-19), never System.currentTimeMillis().

9. STEP-BY-STEP BUILD SEQUENCE: Ordered implementation steps, from empty sensor
   listener to a fully working, independently-testable gesture classifier with a
   test harness that can simulate shake/hold/false-positive inputs without a live
   device for every test run.

10. UNIT + MANUAL TEST PLAN: Concrete test cases — oscillation detector correctly
    rejects a single spike; correctly accepts 2+ reversals; debounce correctly
    collapses a rapid double-shake into one event; state machine correctly
    distinguishes shake vs. shake-and-hold at varying hold durations; three distinct
    false-positive negative tests (walking, pocket, vehicle) per EC-01; a documented
    manual on-device test protocol since accelerometer behavior can't be fully
    simulated in a unit test.

11. EXIT CRITERIA: In the same style as the brief's own Phase 1 exit criteria —
    e.g., "a rapid double-shake within 2s produces exactly one emitted event, not
    two," "a deliberate shake reliably crosses the oscillation threshold across N
    manual trials," "shake-and-hold correctly cancels the in-flight Phase-1 event
    when the hold threshold is crossed," "false-positive rate against the three
    named negative-test categories is documented and within an agreed tolerance."

12. HANDOFF NOTES: What Phase 2 (reconciliation engine, also Parikshit) can assume
    from this phase's output, and the exact event/data contract Phase 3 (Chirag,
    chip UI rendering) needs to implement against.

Format as a complete, professional PRD with numbered sections. Do not restate the
brief or edge-case doc — synthesize into concrete, buildable specification. Address
EC-01, EC-02, EC-03, EC-04, EC-05, EC-19, EC-59 explicitly by ID with the fix baked
into the spec.
```

---

# PHASE 2 — Notification Capture & Event Reconciliation Engine
**Owner: Parikshit · Depends on: Phase 0, Phase 1**

## Prompt to paste (with all 3 files attached)

```
You are generating a standalone, industry-grade Phase PRD for a hackathon project
called Arthix ("Shake & Audit"). Attaching shake-audit-project-brief.md,
edge-case-analysis.md, and techstack.md for full context. Read all three fully first.

CONTEXT: This is Phase 2 of a 7-phase build plan (Chirag: UI/backend/data, Parikshit:
sensors/input, Niranjan: AI/ML/OCR/voice/agentic). Owned SOLELY by Parikshit. Phase 0
(schema, Chirag) and Phase 1 (shake detection, Parikshit) are complete and FROZEN.
Treat as fixed contracts: Phase 0's Transaction/PendingCapture/PendingNotification
Room entities exactly as specified (status enum: confirmed | awaiting_match |
awaiting_category | awaiting_amount | discarded; amount_paise: Long everywhere;
source_capture_id/source_notification_id for traceability); Phase 1's emitted shake
event and shake-and-hold cancellation signal, using SystemClock.elapsedRealtime().
Do not redesign these — build against them.

YOUR TASK: Write a complete, standalone PRD for "Phase 2 — Notification Capture &
Event Reconciliation Engine" (implements FR-2 and FR-2a in full — described in
techstack.md §1.1 as "the highest-leverage piece of code in the project"). This is
the most edge-case-dense phase in the entire build; be exhaustive. Cover:

1. SCOPE: NotificationListenerService setup and notification parsing (FR-2), plus
   the full two-queue reconciliation engine that matches Phase 1's shake events to
   parsed payment notifications (FR-2a). This phase owns the single serial
   CoroutineDispatcher that both event sources feed into.

2. NOTIFICATIONLISTENERSERVICE SETUP: Manifest declaration, permission request flow,
   the onListenerConnected lifecycle gap (EC-13 — specify a "reconnecting..." state
   shown on app start rather than assuming instant binding), and the onboarding
   screen requirement that explains the permission's purpose BEFORE Android's own
   alarming system dialog appears (EC-58) — specify this as a real screen with real
   copy, not a placeholder.

3. PACKAGE ALLOW-LIST — SECURITY BOUNDARY: This is the actual correctness/security
   boundary per EC-08 and EC-56, not the regex. Specify the exact allow-list of UPI
   app package names to check via StatusBarNotification.getPackageName() BEFORE any
   text parsing occurs (GPay: com.google.android.apps.nbu.paisa.user, PhonePe:
   com.phonepe.app, Paytm: net.one97.paytm, and instruct Parikshit to verify/update
   these against the actual demo device's installed apps during Phase 6/7). State
   as a hard rule: non-allow-listed notification content is never logged, cached, or
   retained, even transiently, even for debugging (EC-56).

4. NOTIFICATION TEXT EXTRACTION: Specify reading EXTRA_BIG_TEXT/EXTRA_TEXT_LINES
   first, falling back to EXTRA_TEXT only if unavailable, and flagging a truncated/
   mid-word match as low-confidence rather than committing it (EC-10). Specify
   handling for grouped/bundled notifications via InboxStyle child entries when
   multiple payments arrive quickly (EC-12).

5. PATTERN MATCHING & OUTCOME FILTERING: Specify the regex/pattern set stored in an
   editable JSON config asset (not hardcoded — mitigates EC-09's fundamental,
   unfixable risk of apps changing notification copy). Give concrete example
   patterns for both outflow ("₹450 paid to Ramesh Chai") and inflow ("You received
   ₹500 from Aman") directions. Specify the outcome-keyword filter that MUST run
   before treating any match as a completed outflow — reject/route out declined,
   failed, pending, cancelled, reversed — and specify refund handling as netting
   against the original transaction (same payee + amount) rather than logging as an
   unrelated inflow (EC-07). Specify the shared AmountParser utility call (from
   Phase 0's contract) for normalizing ₹/Rs./INR and separator variants (EC-11).

6. DEDUP LOGIC (EC-06): Before committing a new record from a matched notification,
   specify the exact check for an existing transaction with matching amount + close
   payee-string match within a short window (define "short" and "close" numerically)
   — treat a second matching notification as confirmation of the same event, not a
   new record.

7. THE RECONCILIATION ENGINE — FULL ALGORITHM SPEC: This is the core deliverable.
   Specify in full, precise, pseudocode-or-better detail:
   a. The two persisted queues (PendingCaptures, PendingNotifications) and their
      exact read/write access pattern through the single serial CoroutineDispatcher
      (EC-18 — state this as a correctness requirement, not an optimization; give
      the actual Kotlin Coroutines construct to use).
   b. Debounce interaction with Phase 1's shake events (already handled in Phase 1,
      but specify how Phase 2 receives the resulting single event).
   c. Nearest-neighbor matching algorithm: for each new outflow notification, search
      unmatched PendingCaptures with timestamp ≤ notification timestamp within the
      2-minute max-delay window, select the closest-in-time unmatched capture.
      Specify exact query pattern against Phase 0's DAO layer.
   d. AMBIGUITY THRESHOLD — give this a concrete, numeric, implementable definition
      per EC-14: compute the time-delta for the best match and the second-best
      match; if the gap between them is below an empirically-tunable threshold
      (give a starting value, e.g. a few seconds, explicitly marked as "tune against
      Phase 7 burst-test data"), treat as ambiguous. Write the actual comparison
      logic, not a description of it.
   e. Disambiguation prompt: specify the one-tap UI trigger contract to Phase 3
      (data shape only — rendering is Chirag's job), and the timeout/fallback
      behavior per EC-15: if ignored after a short timeout, fall back to
      nearest-neighbor best guess, flag the record confidence_flag = auto_resolved
      (per Phase 0's schema), never wait indefinitely, never default with no trace.
   f. Timeout hygiene (EC-17): on a PendingCapture/PendingNotification timing out
      with no match, specify the exact sequence — remove it from the active
      matching pool AND mark the corresponding Transaction status = awaiting_amount
      (or awaiting_category, per which side timed out) in the SAME atomic operation,
      so a stale entry is never matchable later by a later, unrelated notification.
   g. Discard path (EC-16, EC-21, EC-25): specify the "not a transaction" action,
      reachable via both a tap affordance (contract to Phase 3) and later a voice
      intent (contract to Phase 4) — this phase implements the underlying discard
      operation (status = discarded) that both surfaces call into.
   h. Monotonic clock: reiterate and enforce SystemClock.elapsedRealtime() for every
      internal comparison (EC-19); wall-clock time is display-only, never used in
      matching math.
   i. Inflow bypass: specify that inflow notifications are logged directly without
      requiring or waiting on any shake event, and that a shake coinciding with an
      inflow becomes an orphan handled via the same discard mechanism (EC-21).

8. STRESS-CASE HANDLING: Explicitly address EC-20 — 4-5 payments within seconds of
   each other (e.g., splitting a bill) as a first-class scenario this engine must
   handle correctly, not just the 2-3 shake case. Specify what "correct" means here
   in terms of the ambiguity threshold's behavior at this burst scale.

9. TRACEABILITY: Specify that every match writes source_capture_id and/or
   source_notification_id onto the resulting Transaction record (EC-23), per
   Phase 0's schema — this phase is responsible for populating those fields
   correctly at match time.

10. STEP-BY-STEP BUILD SEQUENCE: Ordered steps — build and unit-test the allow-list
    + parser first (in isolation, feedable with mock notification text), then the
    dispatcher/queue plumbing, then nearest-neighbor matching, then ambiguity
    detection, then timeout hygiene, then discard/dedup, in that order, with
    reasoning for why this order minimizes rework.

11. UNIT TEST PLAN: Exhaustive — allow-list rejects non-UPI apps; outcome-keyword
    filter rejects declined/failed/pending/reversed; refund nets correctly; dedup
    collapses app+bank double-notification; nearest-neighbor correctly pairs
    out-of-order arrivals (two shakes 5-15s apart, notifications arriving in
    REVERSED order, per the brief's own Phase 1 exit criteria); ambiguity threshold
    correctly triggers on a deliberately close cluster and correctly does NOT
    trigger on a clearly-separated pair; timeout correctly purges from the active
    pool; discard correctly sets status = discarded; 4-5 payment burst scenario
    (EC-20) as an explicit test case; concurrent-thread stress test simulating
    simultaneous shake-callback and notification-binder-callback writes to confirm
    no race condition under the serial dispatcher.

12. EXIT CRITERIA: Match the brief's own Phase 1 exit criteria precisely, since this
    phase's scope is what those criteria were written against — reproduce and adapt
    each one from shake-audit-project-brief.md §3 Phase 1 exit criteria as a
    checkable item here, plus additions for EC-14's ambiguity definition and EC-20's
    burst case being explicitly tested.

13. HANDOFF NOTES: Exact data/event contract Phase 3 (Chirag) needs for rendering
    the chip UI, disambiguation prompt, and discard action; exact contract Phase 4
    (Niranjan) needs for routing timed-out records into the voice follow-up queue.

Format as a complete, professional PRD. Address every ID in edge-case-analysis.md
Section B and Section C explicitly (EC-06 through EC-23) with the fix baked into the
spec, not mentioned as a caveat.
```

---

# PHASE 3 — UI Layer, Persistence Wiring & Manual/Edit Flows
**Owner: Chirag · Depends on: Phase 0, Phase 2**

## Prompt to paste (with all 3 files attached)

```
You are generating a standalone, industry-grade Phase PRD for a hackathon project
called Arthix ("Shake & Audit"). Attaching shake-audit-project-brief.md,
edge-case-analysis.md, and techstack.md for full context. Read all three fully first.

CONTEXT: This is Phase 3 of a 7-phase build plan (Chirag: UI/backend/data, Parikshit:
sensors/input, Niranjan: AI/ML/OCR/voice/agentic). Owned SOLELY by Chirag. Phase 0
(schema, Chirag, own earlier work) and Phase 2 (reconciliation engine, Parikshit) are
complete and FROZEN. Treat as fixed contracts: the full Phase 0 Room schema; Phase 2's
event contracts for triggering the chip UI, the disambiguation prompt, and the
discard action (specified in Phase 2's own PRD's "Handoff Notes" section — assume that
document exists and defines the exact function signatures/data shapes Chirag's UI
code calls into or receives from).

YOUR TASK: Write a complete, standalone PRD for "Phase 3 — UI Layer, Persistence
Wiring & Manual/Edit Flows" (implements the UI layer subsystem, FR-5, and closes the
single most-cited gap across the entire edge-case doc: the missing edit/delete
affordance, EC-52). Cover:

1. SCOPE: All Jetpack Compose UI screens and the repository IMPLEMENTATION layer
   (Phase 0 defined the interfaces; this phase implements them against the actual
   Room DAOs and wires them to ViewModels per MVVM + Repository pattern, per
   techstack.md §3). Does not include voice/OCR/agent UI (Phase 4/5's concern, though
   this phase must expose the extension points those phases will plug into).

2. ARCHITECTURE: MVVM + Repository pattern mapped onto Capture/Storage/Agent/UI
   subsystems (techstack.md §3) — specify the ViewModel-per-screen structure, how
   ViewModels consume the Phase 0 repository interfaces, and how Flow/StateFlow is
   used to keep UI reactive to Room changes.

3. CHIP UI (rendering half of FR-1, contract from Phase 1 & Phase 2): Specify the
   actual heads-up notification action-button rendering — category options (Food/
   Travel/Shopping/Other), the 2-second auto-dismiss timer implementation, and what
   happens on tap (write to the repository, transitioning Transaction status
   correctly per Phase 0's enum) vs. on timeout-untapped (status = awaiting_amount
   or per Phase 2's contract).

4. DISAMBIGUATION PROMPT UI: Specify the actual one-tap UI surfaced when Phase 2's
   engine flags an ambiguous match — show the ambiguous candidate entries clearly
   enough for the user to pick correctly, and specify what happens if it's ignored
   (defers to Phase 2's timeout/fallback logic — this phase just needs to not block
   or crash if no response arrives).

5. MANUAL CHIP/TEXT FALLBACK (FR-5) — FIRST-CLASS REACHABILITY: Per EC-35, this is
   not "a manual trigger" as a minor feature — specify it as a persistent, always-
   reachable quick-access affordance (e.g., explicitly choose and justify: a
   home-screen widget, a persistent notification action, or an always-visible FAB —
   pick one and justify against the "every other capture mode's failure path
   depends on this" framing from EC-35). Every other phase's degraded path (missed
   shake, low-confidence OCR, failed STT) routes here — specify the exact entry
   point contract those phases can call.

6. TRANSACTION EDIT/DELETE SCREEN — NEW REQUIREMENT NOT IN ORIGINAL FR LIST: This is
   the single highest-priority addition from the entire edge-case doc (EC-52). Fully
   specify: a transaction list/history screen (queryable, sortable by date/category),
   tap-to-open a transaction, editable fields (amount, category, payee), a delete
   action with confirmation, and — critically — specify what happens to
   confidence_flag and status when a user manually corrects an auto-resolved or
   needs_review record (should become clean/confirmed). Also specify: per EC-40,
   whether SplitRecord amounts recalculate live or stay locked when the underlying
   Transaction is corrected here — pick the "live" default per techstack.md §3.1's
   recommendation and specify the exact propagation logic.

7. REPORT DISPLAY SCREEN (rendering shell only — content comes from Phase 5): Specify
   the screen layout and the exact data contract/interface it expects to receive
   from Phase 5's Report entity (category_breakdown, net_flow, suggestions,
   projected_total, projected_savings, uncategorized_total_paise) — this phase
   builds the rendering shell against a defined shape even though Phase 5 fills it.

8. NEEDS-REVIEW / CONFIDENCE-FLAG SURFACING: Per EC-22, EC-15, EC-30, EC-32's shared
   principle — specify how the UI visibly distinguishes a clean match from an
   auto_resolved or needs_review one anywhere it's shown (list view, detail view),
   so a silently-wrong auto-match doesn't look identical to a certain one.

9. ONBOARDING SCREENS: Specify the notification-listener permission explainer screen
   (before Android's own alarming system dialog, per EC-58 — real copy, not a
   placeholder) and the battery-optimization whitelist request screen (supports
   Phase 1/EC-59's mitigation) — both belong in Chirag's UI module.

10. EMPTY-STATE HANDLING: Specify sensible empty states — no transactions yet, no
    pending items, first-run state — addressing the "confirm behavior when store is
    empty" reliability requirement from the brief's own Phase 5 test plan.

11. WORKMANAGER SCHEDULING SETUP: Specify the WorkManager configuration this phase
    sets up for idle-detection polling (which Phase 4's voice follow-up trigger will
    use) — this phase wires the scheduling infrastructure; Phase 4 defines what runs
    on it.

12. STEP-BY-STEP BUILD SEQUENCE and 13. UNIT/UI TEST PLAN (Compose UI tests for chip
    rendering, edit screen CRUD roundtrip, empty states) and 14. EXIT CRITERIA
    (checkable, e.g. "a user can view, edit, and delete any committed transaction,"
    "the manual fallback is reachable from the home screen in ≤1 tap," "an
    auto-resolved match visibly differs from a clean match in the UI") and
    15. HANDOFF NOTES (what Phase 4, 5, 6 can plug into) — write these with the same
    rigor and specificity as the other phases.

Format as a complete, professional PRD. Address EC-04, EC-15, EC-16, EC-22, EC-25,
EC-30, EC-32, EC-35, EC-40, EC-52, EC-58 explicitly by ID with the fix baked into
the spec.
```

---

# PHASE 4 — Voice Capture, OCR & Intent Recognition
**Owner: Niranjan · Depends on: Phase 0, Phase 3**

## Prompt to paste (with all 3 files attached)

```
You are generating a standalone, industry-grade Phase PRD for a hackathon project
called Arthix ("Shake & Audit"). Attaching shake-audit-project-brief.md,
edge-case-analysis.md, and techstack.md for full context. Read all three fully first.

CONTEXT: This is Phase 4 of a 7-phase build plan (Chirag: UI/backend/data, Parikshit:
sensors/input, Niranjan: AI/ML/OCR/voice/agentic). Owned SOLELY by Niranjan. Phase 0
(schema), Phase 2 (reconciliation engine's discard/timeout contracts), and Phase 3
(chip UI, manual fallback entry point, edit screen) are complete and FROZEN. Treat as
fixed: Phase 0's Transaction schema (status enum including awaiting_amount and
awaiting_category, confidence_flag enum); Phase 3's chip UI rendering contract (this
phase pre-fills into it for OCR, per FR-4) and manual-fallback entry point (this
phase routes low-confidence OCR/STT results into it, per EC-27/EC-31).

YOUR TASK: Write a complete, standalone PRD for "Phase 4 — Voice Capture, OCR &
Intent Recognition" (implements FR-3, FR-4, and the voice-intent-recognition half of
FR-6). Cover:

1. SCOPE: On-device speech-to-text (Vosk), on-device OCR (ML Kit Text Recognition
   v2), the idle-detection trigger logic for batched voice follow-up, and intent
   parsing (category, amount, discard, contact-name) from both spoken and OCR input.
   Does not include the agent/report LLM (Phase 5) or the split UI itself (Phase 6,
   though this phase produces the parsed voice-split intent Phase 6 consumes).

2. VOSK SETUP: Specify the exact Vosk integration — small Indian-English model
   selection, Kotlin/Android bindings setup, offline-by-construction confirmation
   (why this was chosen over Android's built-in SpeechRecognizer per techstack.md
   §2 — device/OEM-dependent offline support compounds EC-59's OriginOS risk).
   Specify how confidence scores are extracted and exposed from Vosk's output, since
   downstream logic depends on them.

3. IDLE-DETECTION TRIGGER (FR-3): Specify the exact trigger condition using the
   WorkManager scheduling infrastructure Phase 3 set up — "low accelerometer
   variance for N minutes AND at least one awaiting_amount/awaiting_category record
   exists." Critically, address EC-26: require the screen to have been recently
   unlocked/interacted with (not accelerometer stillness alone — a charging phone
   overnight or a desk-idle phone must not trigger), AND check Do Not Disturb/ringer
   state and suppress or defer the prompt if the phone is silenced. Specify the exact
   conditions and the check order.

4. VOICE FOLLOW-UP PROMPT LOGIC — AMOUNT RESOLUTION (EC-24, the single biggest gap
   in the original FR-3 spec): Specify the full prompt flow. If the pending record
   has status = awaiting_amount (no amount at all — a shake that timed out with zero
   notification match), the prompt MUST ask for the amount first or alongside
   category ("how much did you spend at the last one?") — specify the exact prompt
   sequencing logic and how the spoken amount gets parsed into amount_paise (reuse
   Phase 0's AmountParser contract, adapted for spoken-number parsing, e.g. "four
   fifty" → 45000 paise — specify this number-word parsing explicitly, it is
   non-trivial). If status = awaiting_category only, ask for category alone.

5. DISCARD-INTENT RECOGNITION (EC-25): Specify explicit voice-recognizable phrasing
   ("skip," "not real," "ignore that one," and reasonable variants) recognized as a
   distinct intent from a category/amount answer, routing to the same discard
   operation Phase 2 implemented (status = discarded). Give the actual phrase-list/
   matching approach (keyword set, not full NLU, given hackathon time constraints).

6. STT CONFIDENCE GATING (EC-27): Specify the exact threshold-based behavior — below
   threshold confidence, re-prompt once with a clarifying re-ask; if still low
   confidence after the retry, fall back to Phase 3's manual/tap chip UI for that
   specific record rather than committing a guess. Give a concrete starting
   confidence threshold value.

7. CATEGORY TAXONOMY PROTECTION (EC-28): Specify fuzzy-matching logic for free-text
   spoken responses outside Food/Travel/Shopping/Other — normalize to nearest
   existing category, store the original spoken phrase as a sub-tag/note field
   rather than creating a new top-level category. Specify the matching approach
   (simple keyword/similarity match given time constraints — name the actual
   technique, e.g. Levenshtein distance against a small fixed category-keyword list).

8. CODE-SWITCHING / LANGUAGE SCOPE (EC-29): Explicitly scope and document which
   language(s) this build supports for the demo (the persona plausibly code-switches
   Hindi-English) — specify a concrete test set of realistic code-switched phrases to
   validate against before Phase 7, and state plainly if full code-switch support is
   out of scope for the hackathon build (a stated limitation, not a silent gap, in
   the same tone as the edge-case doc's "accepted limitation" items).

9. VOICE-TRIGGERED SPLIT INTENT PARSING (FR-6 voice alt-flow, contract to Phase 6):
   Specify parsing of spoken contact-name intent ("split with Aman and Priya") using
   the same Vosk pipeline — output a structured intent (list of recognized contact
   name candidates) that Phase 6 consumes to build the split record. Specify
   unrecognized-name fallback (routes to Phase 6's tap UI per the brief) and, per
   EC-36, ambiguous-name handling (a spoken name matching multiple saved contacts) —
   this phase's job is to surface the ambiguity (multiple candidate matches) in its
   output intent; Phase 6 renders the tap-to-pick resolution UI.

10. CAMERA OCR PIPELINE (FR-4): Specify CameraX capture setup, ML Kit Text
    Recognition v2 integration (on-device, offline, bundled model — no network call,
    satisfies NFR-1). Specify the AMOUNT RESOLUTION heuristic in full per EC-30:
    keyword-proximity search for "Total"/"Grand Total"/"Amount Payable" first;
    fallback to largest number on receipt ONLY if no keyword match found, and flag
    that fallback case low-confidence explicitly. Specify SANITY BOUNDS per EC-32:
    reject/flag extracted amounts outside a plausible range (give concrete bounds,
    e.g. <₹1 or >₹50,000) for manual confirmation before commit — ₹ symbol and
    decimal-point OCR misreads can be off by orders of magnitude. Specify vendor-name
    handling per EC-33: leave payee blank rather than inserting garbage OCR text if
    no business name is found, let the user fill it via Phase 3's pre-fill
    confirmation chip. Specify handling for low/no-confidence OCR per EC-31 (faded
    thermal paper, handwritten receipts are common for this exact persona) — fall
    back to the manual chip UI pre-filled with whatever partial data was extracted,
    never fail silently or force a full blind re-entry. Specify an explicit OCR
    latency target per EC-34 (a few seconds, tested on the actual demo device) since
    NFR-4's 15s budget is report-only and doesn't cover OCR.

11. STEP-BY-STEP BUILD SEQUENCE: Suggest building OCR first (more deterministic,
    faster to validate against NFR-3's 5-sample-receipt requirement) before voice
    (more edge-case-dense), with reasoning.

12. UNIT + MANUAL TEST PLAN: Vosk confidence-gating test cases; amount-resolution
    from spoken numbers test set; discard-intent recognition test set; OCR against
    the required 5+ varied receipt photos (different lighting, handwriting vs print,
    per NFR-3 and the brief's own Phase 5 test plan); OCR sanity-bounds rejection
    test cases; background-noise voice test (per the brief's own Phase 5 plan).

13. EXIT CRITERIA: Match and adapt the brief's own Phase 2 exit criteria (a
    photographed receipt produces a pre-filled chip confirmation; idle period with
    pending records triggers a working voice follow-up that correctly updates those
    records) plus additions for amount-resolution-via-voice and discard-intent
    working correctly.

14. HANDOFF NOTES: Exact intent/data contract Phase 5 needs (none directly — Phase 5
    consumes committed Transactions, not raw voice/OCR output) and exact contract
    Phase 6 needs for the parsed split-intent.

Format as a complete, professional PRD. Address EC-24 through EC-34 explicitly by ID
with the fix baked into the spec, not mentioned as a caveat.
```

---

# PHASE 5 — Agentic Report Layer (Categorization, Projections, LLM Phrasing)
**Owner: Niranjan · Depends on: Phase 0, Phase 3**

## Prompt to paste (with all 3 files attached)

```
You are generating a standalone, industry-grade Phase PRD for a hackathon project
called Arthix ("Shake & Audit"). Attaching shake-audit-project-brief.md,
edge-case-analysis.md, and techstack.md for full context. Read all three fully first.

CONTEXT: This is Phase 5 of a 7-phase build plan (Chirag: UI/backend/data, Parikshit:
sensors/input, Niranjan: AI/ML/OCR/voice/agentic). Owned SOLELY by Niranjan. Phase 0
(schema) and Phase 3 (report screen rendering shell, expecting a defined Report data
shape) are complete and FROZEN. Treat as fixed: Phase 0's Report entity
(period_start, period_end, category_breakdown{}, net_flow, suggestions[],
projected_total, projected_savings, uncategorized_total_paise per techstack.md
§3.1); Phase 3's report screen expects exactly this shape.

YOUR TASK: Write a complete, standalone PRD for "Phase 5 — Agentic Report Layer"
(implements FR-7 in full). This is the phase where a real, hard architectural
principle from techstack.md §0.2 must be enforced without exception: "anything that
touches money or matching logic is deterministic code you can unit test — anything
that touches phrasing is the only place an LLM is allowed to run." Cover:

1. SCOPE: Categorization refinement, deterministic sum/projection computation, and
   LLM-based report phrasing. This phase does NOT touch the reconciliation engine's
   categorization at capture time (Phase 2) — it refines/aggregates already-committed
   Transaction data for reporting purposes only.

2. TRIGGER HANDLING: Specify both trigger paths — scheduled (daily/weekly, via
   WorkManager) and manual shake-and-hold (consuming Phase 1's shake-and-hold event
   contract). Specify PERIOD SEMANTICS explicitly per EC-50: pin down whether a
   daily-triggered report compares day-over-day or always week-over-week regardless
   of trigger source (pick one, e.g. "always week-over-week, with 'today so far' as
   a separate line" per the edge-case doc's own suggested resolution, and justify).

3. DETERMINISTIC COMPUTATION LAYER (build this FIRST, in plain Kotlin, fully unit
   tested BEFORE any LLM code is written): 
   a. Category breakdown: sum amount_paise grouped by category within the period,
      querying via Phase 0's repository/DAO layer.
   b. Pending/unlabeled inclusion (EC-44): specify that pending/unlabeled amounts
      MUST be included in the overall total (never invisible money) but EXCLUDED
      from category-specific suggestions until resolved — give the exact
      "₹340 uncategorized" style output pattern.
   c. Zero-baseline handling (EC-45): specify explicit branch logic for "no prior
      period data" as its own output case (e.g., "first week of tracking" message)
      rather than letting a week-over-week percentage produce NaN/Infinity.
   d. PROJECTION ANCHORING (EC-43, critical): specify the exact anti-naive-
      extrapolation algorithm — do NOT simply compute (spend-so-far ÷ time-elapsed)
      × period-length. Specify a blended approach: anchor to the Phase 7 pre-seeded
      historical baseline, blending recent live-session activity with established
      historical pattern rather than extrapolating off a thin/early sample. Give the
      actual blending formula or weighting approach to use, concrete enough to
      implement (e.g., a weighted average between historical daily average and
      current-period daily average, with weights that shift as more of the current
      period's data accumulates).
   e. Suggestion generation: specify the deterministic rule for producing at least
      one cut-down suggestion referencing actual logged categories/amounts (e.g.,
      "category X totals ₹Y this week, Z% above baseline") — this is a computed,
      templated data structure (category name, amount, percentage, comparison
      baseline), NOT free text yet at this stage.
   f. Rounding/currency handling: reiterate integer paise arithmetic throughout,
      only converting to rupee display strings at the final rendering step.

4. LLM PHRASING LAYER — GROUNDING PATTERN (implements EC-48, the second most
   important architectural safeguard in this whole build): Specify the exact
   pipeline: 
   a. Pass ONLY the verified numbers computed in step 3 into the LLM prompt, with
      an explicit system/prompt instruction to phrase language around them and never
      invent or restate a different figure. Give an actual example prompt template.
   b. POST-GENERATION VALIDATION: specify a lightweight post-check that parses every
      number appearing in the generated text and confirms each one matches a value
      from the whitelist passed in. Specify exact behavior on validation failure:
      fall back to a template sentence built from the same numbers (give an example
      template sentence) rather than rendering unvalidated model output. This must
      be a real, implementable check (e.g., regex-extract numeric tokens from
      output, compare against the whitelist set) — not "ask the model nicely again."
   c. Model choice: MediaPipe LLM Inference API running Gemma 3 1B or Gemma 2 2B,
      int4/int8 quantized, on-device. Specify integration steps, model bundling
      approach, and memory/footprint considerations for the demo device.

5. LATENCY BUDGET (NFR-4, EC-49): Specify how the grounding approach keeps this fast
   — the model only phrases a few sentences around known numbers rather than
   reasoning through arithmetic. Specify a concrete latency test protocol on the
   actual demo device, and specify what happens if the 15-second budget is at risk
   (e.g., a hard timeout with fallback to the fully-templated non-LLM sentence from
   step 4b, so the demo never hangs).

6. OFFICE KIT BRIDGE — ADDITIVE, NEVER LOAD-BEARING (EC-47, critical): Specify the
   pluggable strategy interface for optionally routing report generation through
   Green Light compute via the Office Kit bridge, explicitly designed so the
   on-device path in steps 3-5 is the default and works fully standalone. Specify
   that this satisfies Constraint 1.4 (no laptop-availability assumption) and
   Success Criterion 1.5 (demonstrable live on-device) from the brief, while the
   bridge path exists purely as an additive enhancement for the separate "Office Kit
   usage" rubric line. Specify the interface/strategy-pattern shape so swapping
   between on-device and bridge-backed generation doesn't change any calling code.

7. SPLITWISE GROUP-SUGGESTION HEURISTIC (the non-ML half of FR-6, since this is
   deliberately rule-based per techstack.md §2's "Reconciliation 'agent' and
   split-group suggestion: deliberately not ML"): Specify the category-match +
   time-of-day/day-of-week pattern heuristic against contact history, and the
   explicit COLD-START DEFAULT per EC-41: no suggestion offered, prompt the user to
   pick manually, when there's no history to draw on — never guess with no basis.
   (Note: the split UI/confirmation itself is Phase 6's job; this phase only
   produces the suggested group as a data output Phase 6 consumes.)

8. STEP-BY-STEP BUILD SEQUENCE: Deterministic computation layer first, fully unit
   tested and verified against hand-computed expected values, BEFORE any LLM
   integration work begins — reiterate why this order matters (a report that's
   arithmetically wrong is worse than one that's unphrased).

9. UNIT + VALIDATION TEST PLAN: Category-sum correctness against hand-built test
   datasets (manually verify category sums, net flow, and projections against raw
   transaction data for at least 2 test datasets, per the brief's own Phase 5 test
   plan); zero-baseline branch test; projection-anchoring test with a deliberately
   thin/early dataset to confirm it does NOT produce an absurd extrapolated number;
   LLM output validation-check test (deliberately feed a mismatched number to
   confirm the fallback template triggers correctly); latency benchmark on-device.

10. EXIT CRITERIA: Match and extend the brief's own Phase 3 exit criteria — running
    the agent against a populated local store produces a report with correct
    arithmetic (breakdown sums match raw data) and a suggestion referencing an
    actual category/amount, not generic text — plus explicit additions: zero-
    baseline case handled without error, thin-data projection does not produce an
    absurd figure, LLM validation-fallback path verified working, report generation
    completes under 15s across repeated on-device trials.

11. HANDOFF NOTES: Confirm the exact Report data shape delivered matches Phase 3's
    rendering-shell expectation field-for-field; confirm the split-group suggestion
    output shape Phase 6 will consume.

Format as a complete, professional PRD. Address EC-43 through EC-50 explicitly by ID
with the fix baked into the spec, not mentioned as a caveat.
```

---

# PHASE 6 — Split Feature (Splitwise-style)
**Owner: Chirag · Depends on: Phase 0, Phase 3, Phase 4 (voice-split intent), Phase 5 (group suggestion)**

## Prompt to paste (with all 3 files attached)

```
You are generating a standalone, industry-grade Phase PRD for a hackathon project
called Arthix ("Shake & Audit"). Attaching shake-audit-project-brief.md,
edge-case-analysis.md, and techstack.md for full context. Read all three fully first.

CONTEXT: This is Phase 6 of a 7-phase build plan (Chirag: UI/backend/data, Parikshit:
sensors/input, Niranjan: AI/ML/OCR/voice/agentic). Owned SOLELY by Chirag. Phase 0
(schema), Phase 3 (UI patterns/edit screen), Phase 4 (voice-split intent parsing
output), and Phase 5 (group-suggestion heuristic output) are complete and FROZEN.
Treat as fixed: Phase 0's SplitRecord entity (transaction_id, participants[],
share_per_person_paise, confirmed_via: [tap | voice], amount_lock: [live |
locked_at_creation] — techstack.md §3.1 recommends "live" as the default, meaning
splits recalculate if the underlying transaction is later corrected; treat this as
the decided default unless this phase's PRD explicitly re-justifies overriding it);
Phase 4's parsed voice-split intent output (list of recognized/ambiguous contact-name
candidates); Phase 5's group-suggestion output (a suggested contact group or an
explicit "no suggestion, cold start" signal).

YOUR TASK: Write a complete, standalone PRD for "Phase 6 — Split Feature
(Splitwise-style)" (implements FR-6 in full, the tap-UI and confirmation half — the
voice-intent parsing itself was Phase 4's job, the group-suggestion heuristic was
Phase 5's job; this phase is the UI, confirmation flow, and split-math implementation
that ties both together). Cover:

1. SCOPE: The "Split with?" prompt UI (triggered after any transaction commit), the
   group-suggestion display (consuming Phase 5's output), the tap-to-edit
   participant/share UI, the voice-alt-flow confirmation UI (consuming Phase 4's
   parsed intent), and the split-math implementation (even-split default, rounding
   rule, custom proportions via tap).

2. POST-COMMIT SPLIT PROMPT: Specify exactly when/how the "Split with?" prompt
   surfaces after any transaction commit (from any capture path — shake, OCR, voice,
   manual) — this should be a single shared trigger point regardless of how the
   transaction was created.

3. GROUP SUGGESTION DISPLAY: Specify how Phase 5's suggested-group output (or its
   explicit cold-start "no suggestion" signal, EC-41) is rendered — when a
   suggestion exists, show it pre-selected but editable; when cold-start applies,
   show an empty/manual-pick state with no guessed default, per EC-41's explicit
   rule that the agent must never guess with no basis.

4. TAP-TO-EDIT PARTICIPANT/SHARE UI: Specify the full editable split UI — add/remove
   participants, adjust individual shares, with even-split as the default starting
   point. Specify NON-CONTACT PARTICIPANT support per EC-37: allow adding an ad hoc
   name directly (not pulled from the phonebook) for one-off splits with someone not
   saved in contacts.

5. ROUNDING REMAINDER RULE (EC-38, concrete and non-optional): Specify the exact,
   explicit rule for a non-divisible even split (e.g., ₹100 ÷ 3 = ₹33.33... with a
   leftover paisa) — per the edge-case doc's suggested resolution, the payer absorbs
   the rounding remainder, added to their own share. Specify the exact arithmetic
   using integer paise (per Phase 0's schema) to avoid float drift, and give a
   worked numeric example in the spec itself.

6. VOICE-ALT-FLOW CONFIRMATION (consuming Phase 4's output): Specify how a parsed
   voice-split intent ("split with Aman and Priya") gets rendered as a
   tap-to-confirm summary BEFORE finalizing — per EC-42, this is non-negotiable:
   never auto-commit a voice-parsed split silently, since a misheard name or wrong
   share has real-world consequences outside the app (someone gets over/under-
   charged) even though no real money moves through Arthix itself. Specify exact UI:
   show all parsed participants and computed shares, require an explicit confirm tap.

7. AMBIGUOUS CONTACT-NAME RESOLUTION (EC-36): When Phase 4's output indicates a
   spoken name matched multiple saved contacts, specify the tap-to-pick disambiguation
   UI this phase renders (surface both/all candidate matches, let the user pick —
   never silently guess the first/most-recent match).

8. VOICE SPLIT SCOPE — EVEN-SPLIT ONLY (EC-39): Explicitly document (in-product, not
   just in this PRD) that voice-triggered splits are even-split only; custom
   proportions ("split 60-40") require the tap UI. Specify where/how this constraint
   is communicated to the user if they attempt an unsupported voice command.

9. SPLIT-AMOUNT LIVE RECALCULATION (EC-40): Since Phase 0's schema defaults to
   amount_lock = live, specify the exact propagation logic here: when a transaction's
   amount is corrected later (via Phase 3's edit screen, e.g. resolving a previously-
   pending amount through Phase 4's voice follow-up), specify how the associated
   SplitRecord's share_per_person_paise values are recalculated automatically, and
   specify how this is surfaced to the user (should a corrected split silently
   change, or should it flag itself as "recalculated" in the UI? Pick one and justify
   — recommend flagging, consistent with Phase 3's confidence-flag surfacing pattern
   from EC-22).

10. SPLIT MATH UNIT: Specify the core split-calculation function (pure Kotlin,
    testable in isolation) — inputs: total amount_paise, participant count or
    explicit per-person overrides; output: exact per-person share_per_person_paise
    values with the rounding-remainder rule from step 5 applied deterministically.

11. STEP-BY-STEP BUILD SEQUENCE: Split-math unit (pure logic, testable without UI)
    first, then the tap UI, then group-suggestion integration, then voice-confirmation
    UI last (since it depends on Phase 4's output format being finalized).

12. UNIT + UI TEST PLAN: Rounding-remainder correctness across several participant
    counts and amounts; non-contact participant addition; ambiguous-name tap
    resolution; voice-split confirm-before-commit (confirm nothing commits without
    the explicit tap); live-recalculation propagation test (edit a transaction
    amount, confirm the linked split updates and flags itself); cold-start no-
    suggestion state test.

13. EXIT CRITERIA: e.g., "a split can be created via tap with a correct even split
    including remainder handling," "a voice-parsed split requires and correctly
    waits for explicit tap confirmation before committing," "an ambiguous spoken
    name surfaces a correct disambiguation choice," "editing a transaction's amount
    correctly recalculates and flags its linked split."

14. HANDOFF NOTES: None required downstream — this is a leaf feature — but confirm
    final SplitRecord write-back format matches Phase 0's schema exactly.

Format as a complete, professional PRD. Address EC-36 through EC-42 explicitly by ID
with the fix baked into the spec, not mentioned as a caveat.
```

---

# PHASE 7 — Integration, Test Pass & Demo Readiness
**Owner: All three, jointly · Depends on: everything above**

## Prompt to paste (with all 3 files attached)

```
You are generating a standalone, industry-grade Phase PRD for a hackathon project
called Arthix ("Shake & Audit"). Attaching shake-audit-project-brief.md,
edge-case-analysis.md, and techstack.md for full context. Read all three fully first.

CONTEXT: This is Phase 7, the final phase of a 7-phase build plan (Chirag: UI/
backend/data, Parikshit: sensors/input, Niranjan: AI/ML/OCR/voice/agentic). Owned
JOINTLY by all three teammates — this is where their independently-built,
independently-tested modules (Phases 0-6) get wired together, stress-tested as a
whole system, and made demo-ready. Treat all prior phases' outputs as complete and
functioning in isolation; this phase's job is integration-level correctness, which
by definition cannot be verified by any one person testing only their own module.

YOUR TASK: Write a complete, standalone PRD for "Phase 7 — Integration, Test Pass &
Demo Readiness" combining the brief's own Phase 4 (UI Integration), Phase 5 (Test
Phase), and Phase 6 (Demo Readiness) into one coherent, execution-ready document,
enriched with every relevant edge case. Cover:

1. SCOPE: Full end-to-end wiring of all capture paths (FR-1 through FR-5) into a
   single consistent flow with no dead ends or unhandled states; full functional,
   reliability, and performance test pass across the integrated system; pre-seeded
   demo data; final demo rehearsal and device-specific checklist.

2. INTEGRATION WIRING CHECKLIST: A concrete, ordered checklist of every cross-module
   connection point that must be verified once all modules are merged — enumerate
   each one explicitly by referencing the Handoff Notes sections of Phases 0-6 (e.g.,
   "Phase 1's shake event correctly triggers Phase 3's chip UI," "Phase 2's
   disambiguation flag correctly renders in Phase 3's UI," "Phase 4's OCR pre-fill
   correctly populates Phase 3's chip confirmation," "Phase 4's voice-split intent
   correctly feeds Phase 6's confirmation UI," "Phase 5's Report shape exactly
   matches Phase 3's rendering expectations," "Phase 6's split-math correctly reads
   from and writes to Phase 0's schema"). Specify that each connection point gets an
   explicit pass/fail check before broader testing begins, not just an "it compiles"
   assumption.

3. FULL END-TO-END FLOW VERIFICATION: Specify the exact flow the brief's own Phase 4
   exit criteria requires: payment notification → shake → chip tap → (later)
   shake-and-hold → full report, entirely within the app, no dead ends or unhandled
   states — as a scripted manual test walkthrough with explicit checkpoints.

4. FUNCTIONAL TEST SUITE (adapt and expand the brief's own Phase 5 test plan):
   a. Shake detection: false positives (walking, pocket, vehicle — three distinct
      cases per EC-01) and false negatives (deliberate shake not registering, EC-02).
   b. Reconciliation engine: concurrent-payment scenarios explicitly — 2-3 shakes in
      quick succession with notifications delayed and arriving out of order; confirm
      nearest-neighbor pairing, independent per-capture timeout, and disambiguation
      prompt triggering only in genuinely ambiguous clusters. INCLUDE the 4-5
      transaction burst stress case explicitly (EC-20), not just the 2-3 case.
   c. Notification parsing: at least 3 different UPI app notification formats (real
      or documented mock), covering both inflow and outflow, INCLUDING declined/
      failed/pending/refund cases (EC-07) as explicit negative test cases.
   d. OCR: 5+ varied receipt photos (different lighting, handwriting vs print, per
      NFR-3), including at least one deliberately low-confidence case verifying the
      manual-fallback route (EC-31).
   e. Voice follow-up: background noise present; confirm correct category AND
      amount mapping (EC-24); confirm discard-intent recognition (EC-25); confirm
      confidence-gated re-prompt/fallback behavior (EC-27).
   f. Voice-triggered split: spoken contact names correctly map to intended group,
      INCLUDING an unrecognized-name case (falls back to tap UI, not silent failure)
      and an ambiguous-name case (EC-36).
   g. Agent output: manually verify category sums, net flow, and projections against
      raw transaction data for at least 2 test datasets; verify zero-baseline
      handling; verify thin-data projection does not produce an absurd figure
      (EC-43); verify LLM output-validation fallback triggers correctly on a forced
      mismatch test.
   h. Edit/delete: verify a committed transaction can be corrected, and that a
      linked SplitRecord correctly recalculates and flags itself (EC-40).
   i. Discard path: verify a false-positive shake, an orphaned inflow-coincident
      shake (EC-21), and a voice "skip" all correctly resolve to status = discarded
      with no ledger corruption.

5. RELIABILITY TEST SUITE: Full flow run end-to-end at least 5 consecutive times
   without crash or stuck state; confirm sensible empty-state behavior when the
   store is empty; confirm app-restart recovery correctly reloads persisted pending
   queues (EC-51) rather than losing in-flight reconciliation state; confirm
   NotificationListenerService's onListenerConnected reconnection state (EC-13)
   doesn't drop payments made in the reconnection gap during a restart test.

6. PERFORMANCE TEST SUITE: Shake-to-chip latency stays under 3s across repeated
   trials (NFR-2); OCR extraction latency target met on the actual demo device
   (EC-34); report generation stays under 15s across repeated trials (NFR-4),
   including a forced-timeout test confirming the LLM fallback-to-template path
   engages correctly rather than hanging.

7. PERMISSION-STATE VERIFICATION CHECKLIST (EC-60): A pre-demo checklist item
   distinct from general rehearsal — verify notification listener access,
   microphone, camera, contacts, and battery-optimization whitelist are ALL still
   granted on the actual demo device immediately before going on stage, since any
   one silently missing/revoked breaks its corresponding path without obvious
   symptoms.

8. DEMO DATA SEEDING (brief's own Phase 6): Specify a concrete pre-seeded dataset —
   multiple categories, at least one week of simulated history — realistic enough
   that Phase 5's report has meaningful data to reason over, and specify that the
   live demo flow should visibly build ON TOP of this history (supports EC-43's
   projection-anchoring approach) rather than starting from zero.

9. SIMULATED-NOTIFICATION FALLBACK RELIABILITY (EC-61): Specify that the simulated-
   UPI-notification tooling (the fallback if a live bank transaction isn't available
   on stage) must be rehearsed exactly as much as the real path — not treated as a
   lesser/backup demo mode, since Phase 1's own exit criteria explicitly allow "real
   or simulated" as equally valid.

10. DELIBERATE EDGE-CASE DEMONSTRATION (EC-62): Since "Technical depth — robustness"
    is 15% of the rubric and judges are likely to probe concurrent payments/
    ambiguous matches/false positives directly, specify planning ONE edge case to
    deliberately demonstrate live (e.g., triggering the disambiguation prompt on
    purpose) as part of the pitch, rather than only handling it invisibly in code —
    specify which edge case is the strongest candidate to showcase and why.

11. KNOWN-LIMITATIONS STATEMENT: Specify a short, explicit, team-agreed list of
    accepted limitations to state proactively if asked (not defensively) — no cloud
    sync/backup (EC-55), no at-rest encryption if time didn't allow it (EC-57),
    OriginOS background-kill as a fundamental platform limit with mitigation, not
    elimination (EC-59), the shake-detection precision/recall tradeoff (EC-01/EC-02)
    — framed as "conscious boundary" per the edge-case doc's own tone, not "bug we
    didn't get to."

12. RUN-OF-SHOW / REHEARSAL PLAN: A structured rehearsal schedule — minimum 3
    back-to-back successful full-flow runs on the actual demo device itself (not
    emulator/dev environment), per the brief's own Phase 6 exit criteria, with a
    named owner for each checklist item across the three teammates.

13. EXIT CRITERIA: All functional tests pass; no crash across 5 consecutive
    full-flow runs; latency targets met consistently; flow rehearsed and passes at
    least 3 times back-to-back on the demo device itself; permission-state checklist
    verified immediately pre-demo.

14. ROLE ASSIGNMENT FOR THIS PHASE: Since this phase is joint, specify which person
    is best positioned to own which test category given their module ownership
    (e.g., Parikshit leads reconciliation/sensor stress tests, Niranjan leads
    agent-output and voice/OCR test verification, Chirag leads UI/integration/edit-
    flow tests and owns the overall wiring checklist from section 2), while all
    three participate in the full end-to-end and reliability runs together since
    those require the whole system.

Format as a complete, professional PRD. This phase must reference and close out
essentially every remaining edge case not fully owned by an earlier phase — cross-
check against the edge-case doc's own "Quick reference — the 14 Critical items"
table and confirm each one is covered by name somewhere in this test/readiness plan.
```

---

## Notes on using this document

- **Do not skip Phase 0.** Every other prompt above assumes it exists and is frozen.
  If Chirag hasn't generated and locked it yet, generating Phase 1-6 prompts will
  produce PRDs that silently disagree with each other on the schema.
- **Each generated PRD is meant to stand alone** — the assigned teammate should not
  need to re-read the brief, edge-case doc, or the other phases' PRDs to build their
  phase, though referencing this master doc's dependency table is fine.
- **If a phase's generated PRD reveals a needed change to a frozen earlier contract**
  (this will happen — it's normal), don't silently diverge. Renegotiate explicitly
  with that contract's owner and regenerate the downstream PRD if needed, rather than
  each person quietly assuming their own version of the schema.
- Phase 7's prompt should be run **last**, after Phases 0-6 are at least mostly built,
  since its checklist references each phase's actual handoff notes by name.

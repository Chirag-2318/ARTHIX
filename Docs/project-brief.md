# Shake & Audit — Business & Product Requirements Document

**Track:** FinTech and Commerce · iQOO Hackathon 2026, Pune City Battle
**Team:** Niranjan (agentic/AI), Parikshit (sensors/input), Chirag (UI/app)

---

## 1. BRD — Business Requirements

### 1.1 Problem
Expense tracking fails at the exact moment it matters — right after payment.
Users cannot stop to type or talk (queues, meetings, in motion), so
transactions go unlogged and spending awareness never improves. Existing
"AI expense trackers" bolt a chatbot onto a spreadsheet and ignore this
usability gap.

### 1.2 Business Objective
Build a phone-first expense logger that captures spending with near-zero
friction regardless of physical context, then uses an AI agent to convert
that data into concrete, personalized financial guidance — not a static
categorized list.

### 1.3 Target User
Students and young professionals in India who pay primarily via UPI, split
expenses with friends regularly, and want spending awareness without manual
logging overhead.

### 1.4 Constraints
- Must run natively on Android (iQOO/OriginOS device), on-device inference
  required for all core logging features — no cloud dependency for the
  logging path
- Must function with intermittent or no connectivity for logging features
- Build environment splits into two phases: phone-only and phone+laptop
  (bridged) — architecture must not assume laptop availability at any point

### 1.5 Success Criteria
- A transaction can be logged in under 3 seconds with zero typing/talking
- The AI agent produces a report containing at least one concrete
  personalized suggestion and a projected month-end ₹ figure, derived from
  real logged data (no placeholder text)
- End-to-end flow (payment → log → report) is demonstrable live on-device

---

## 2. PRD — Product Requirements

### 2.1 System Overview
Four subsystems:
1. **Capture layer** — shake detection, notification listener, camera OCR,
   voice capture
2. **Storage layer** — local on-device transaction store
3. **Agent layer** — categorization, splitwise-group matching, report
   generation (suggestions + projections)
4. **UI layer** — chip-tap interaction, report display, split-confirmation
   interface

### 2.2 Functional Requirements

**FR-1: Shake-to-Log**
- System listens for accelerometer shake gesture (defined threshold:
  sustained acceleration spike above X m/s² within Y ms window — tune
  empirically to reject false positives from normal handling)
- On detection, check for a recent (last 60s) UPI payment notification
- If found: display chip UI with categories `Food` `Travel` `Shopping`
  `Other`, auto-dismiss after 2s if untapped
- If tapped: write transaction record (amount, payee, category, timestamp)
  to local store
- If untapped: write transaction record with category = `unlabeled`,
  pending = true

**FR-2: Notification Capture**
- Use Android `NotificationListenerService` to read UPI app notification text
  system-wide (one-time permission grant) — do NOT use screenshot/
  `MediaProjection` capture; it requires repeated permission re-grants and
  needs OCR as an extra failure-prone step for data that's already
  structured text in the notification
- Parse notification text against known UPI payment patterns for both
  directions:
  - Outflow pattern (e.g. "₹450 paid to Ramesh Chai")
  - Inflow pattern (e.g. "You received ₹500 from Aman")
- Extract: amount, payee/sender string, timestamp, direction
  (`inflow` | `outflow`)
- No network call required for this extraction
- Inflow notifications do not require a prior shake — log directly on
  detection, since there's no user-initiated rush moment to anchor to
- No shake→screenshot flow anywhere in this system

**FR-2a: Event Reconciliation Engine (shake ↔ notification matching)**
- Problem: shake (user intent signal) and the UPI notification confirming
  the payment do not arrive at the same time — notification delay can range
  from ~1s to ~2min depending on bank/app. Multiple payments in quick
  succession can also produce multiple pending shakes and pending
  notifications concurrently, arriving out of strict order.
- Maintain two time-ordered queues:
  - `PendingCaptures`: shake events, `{timestamp, matched: bool}`
  - `PendingNotifications`: parsed outflow notifications, `{timestamp,
    amount, payee, matched: bool}`
- On each new shake: debounce — ignore additional shake events within a
  2-second cooldown of the last shake, to prevent one real payment
  producing two records. Then create a `PendingCapture` and immediately
  show the chip UI (category selection does not wait on the notification)
- On each new outflow notification: search unmatched `PendingCaptures`
  with `timestamp ≤ notification.timestamp` within a max-delay window
  (default 2 minutes). Match to the **closest-in-time** unmatched capture
  (nearest-neighbor), not strict FIFO — different banks/apps have
  different delay profiles, so arrival order is not reliable
- On match: bind amount/payee to the corresponding transaction record,
  enriching it whether the user already picked a category or not
- Ambiguity case (multiple unmatched captures and notifications close
  together in time such that nearest-neighbor match is not clearly
  correct): do not silently guess — surface a lightweight one-tap
  disambiguation prompt showing the ambiguous entries so the user confirms
  the correct pairing. This should be a rare path, not the default UX
- Timeout: each `PendingCapture` has its own independent 2-minute timer,
  not blocked by other pending entries. On expiry with no match, mark the
  record `amount: pending` and route it into the batched voice follow-up
  (FR-3) queue

**FR-3: Batched Voice Follow-Up**
- Trigger condition: device idle (low accelerometer variance for N minutes)
  AND at least 1 record with pending = true exists
- Prompt via voice: read out pending transactions one at a time, capture
  spoken response, run on-device speech-to-text, map response to category
  or free-text tag
- Update matching record(s), set pending = false

**FR-4: Camera OCR Logging**
- Manual trigger (button/icon)
- Capture image → on-device OCR → extract amount, vendor name, line items
  if present
- Pre-fill a confirmation chip UI (same as FR-1) before committing

**FR-5: Manual Chip/Text Fallback**
- Manual trigger, same chip UI as FR-1, for silent-environment logging with
  no preceding notification/shake

**FR-6: Splitwise-style Split**
- After any record commit, prompt "Split with?"
- Agent suggests a contact group based on: category match + historical
  time-of-day/day-of-week pattern for that category
- User confirms or edits the group via tap
- **Voice-triggered alt-flow:** user can instead speak the split directly
  (e.g. "split with Aman and Priya") using the same on-device speech-to-text
  pipeline as FR-3; parse the response for contact-name intent and create
  the split record without requiring the tap UI at all
- On confirm (tap or voice): create split record (record_id, participants,
  per-person share — default even split, editable)

**FR-7: AI Spending Agent / Report**
- Trigger: scheduled (daily/weekly) or manual "shake-and-hold" (long shake
  gesture, distinct threshold/duration from FR-1)
- Input: all committed transaction records in the relevant window
- Output:
  - Categorized breakdown, current period vs previous period
  - At least one cut-down suggestion referencing actual logged categories
    and amounts (e.g. category X totals ₹Y this week, Z% above baseline)
  - Projected end-of-period total at current spend rate
  - Projected savings figure if the suggestion is followed
- Report renders on-device; generation may run on Green Light compute via
  Office Kit bridge, but final output must display on-phone

### 2.3 Non-Functional Requirements
- NFR-1: Capture-layer features (FR-1 through FR-5) function fully
  offline, on-device
- NFR-2: Shake-to-chip interaction resolves in ≤3 seconds end-to-end
- NFR-3: OCR extraction accuracy validated against at least 5 sample
  receipts before demo
- NFR-4: Agent report generation must complete in a time acceptable for a
  live demo (target: under 15 seconds from trigger to rendered report)
- NFR-5: No hardcoded/mock report values in the final build — report must
  be generated from actual local transaction data at demo time

### 2.4 Data Model (minimum viable)
```
Transaction {
  id, amount, payee, category, timestamp,
  direction: [inflow | outflow],
  source: [shake | voice | camera | manual],
  pending: bool   // true if amount not yet resolved
}

PendingCapture {
  id, timestamp, matched: bool, category (if already chosen)
}

PendingNotification {
  id, timestamp, amount, payee, matched: bool
}

SplitRecord {
  transaction_id, participants[], share_per_person,
  confirmed_via: [tap | voice]
}

Report {
  period_start, period_end, category_breakdown{},
  net_flow (inflow - outflow),
  suggestions[], projected_total, projected_savings
}
```

### 2.5 Out of Scope (for this build)
- Real payment/UPI integration (no actual money movement — this is a
  logging/advisory layer only)
- Multi-user cloud sync
- Cross-platform (iOS) support

---

## 3. Build Phases

### Phase 1 — Capture Layer (on-device only)
- Implement accelerometer shake detection with tunable threshold and
  2-second debounce cooldown
- Implement notification listener for UPI payment pattern matching
  (both inflow and outflow patterns)
- Implement the event reconciliation engine (FR-2a): `PendingCaptures` and
  `PendingNotifications` queues, nearest-neighbor matching within the
  delay window, per-capture independent timeout, disambiguation fallback
- Implement chip-tap UI component (2s auto-dismiss)
- Implement local transaction store (create/read/update)
- **Exit criteria:**
  - Shaking the phone immediately after a real or simulated UPI outflow
    notification reliably produces a chip prompt and commits a correctly
    populated record
  - A rapid double-shake (within 2s) produces exactly one `PendingCapture`,
    not two
  - Two shakes 5–15 seconds apart, followed by their two corresponding
    notifications arriving out of order (second payment's notification
    arrives first), correctly match each notification to its true shake —
    not by arrival order
  - A `PendingCapture` with no matching notification within the timeout
    window correctly falls through to `pending: true` / routes to the
    voice follow-up queue, without blocking or delaying other pending
    captures
  - A deliberately ambiguous case (two shakes and two notifications
    clustered close enough in time that nearest-neighbor is not clearly
    correct) triggers the disambiguation prompt instead of silently
    guessing
  - An inflow notification is logged directly without requiring or
    waiting on any shake event

### Phase 2 — Secondary Capture Modes
- Implement camera OCR pipeline for receipts
- Implement voice capture + on-device speech-to-text for batched follow-up
- Implement idle-detection trigger for the follow-up flow
- **Exit criteria:** a photographed receipt produces a pre-filled chip
  confirmation; an idle period with pending records triggers a working
  voice follow-up that correctly updates those records

### Phase 3 — Agent Layer
- Implement categorization refinement logic
- Implement splitwise group-suggestion logic (category + time-pattern
  matching against contact history)
- Implement report-generation agent: category breakdown, suggestion
  generation, projection calculation — all derived from real stored data
- **Exit criteria:** running the agent against a populated local store
  produces a report with correct arithmetic (breakdown sums match raw
  data) and a suggestion that references an actual category/amount from
  that data, not generic text

### Phase 4 — UI Integration
- Implement report display screen
- Implement split-confirmation interface
- Wire all four capture modes (FR-1–FR-5) into a single consistent flow
- **Exit criteria:** a user can go from payment notification → shake →
  chip tap → (later) shake-and-hold → full report, entirely within the app,
  with no dead ends or unhandled states

### Phase 5 — Test Phase
- **Functional testing:**
  - Shake detection: test against false positives (walking, pocket
    movement) and false negatives (deliberate shake not registering)
  - Reconciliation engine: test concurrent-payment scenarios explicitly —
    2–3 shakes in quick succession with notifications arriving delayed and
    out of order; confirm correct nearest-neighbor pairing, correct
    independent timeout behavior per capture, and correct disambiguation
    prompt only in genuinely ambiguous clusters
  - Notification parsing: test against at least 3 different UPI app
    notification formats if feasible, or documented mock formats,
    covering both inflow and outflow patterns
  - OCR: test against 5+ varied receipt photos (different lighting,
    handwriting vs print)
  - Voice follow-up: test with background noise present, confirm correct
    mapping of spoken response to transaction
  - Voice-triggered split: confirm spoken contact names correctly map to
    the intended split group, including a case with an unrecognized name
    (should fall back to tap UI rather than silently failing)
  - Agent output: manually verify category sums, net flow (inflow minus
    outflow), and projections against raw transaction data for at least
    2 test datasets
- **Reliability testing:**
  - Full flow run end-to-end at least 5 consecutive times without crash
    or stuck state
  - Confirm behavior when store is empty (no transactions yet) — no
    crashes, sensible empty state
- **Performance testing:**
  - Confirm shake-to-chip latency stays under 3s across repeated trials
  - Confirm report generation stays under 15s across repeated trials
- **Exit criteria:** all functional tests pass, no crash across 5
  consecutive full-flow runs, latency targets met consistently

### Phase 6 — Demo Readiness
- Populate the store with realistic pre-seeded transactions covering
  multiple categories and at least one week of simulated history, so the
  report has meaningful data to reason over
- Confirm the full flow (real shake → real chip tap → real report) runs
  live and reliably on the actual device to be used for the demo
- **Exit criteria:** flow rehearsed and passes at least 3 times back-to-back
  on the demo device itself, not just a dev/emulator environment

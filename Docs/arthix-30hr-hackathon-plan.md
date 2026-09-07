# ARTHIX — 30-Hour Hackathon Execution Plan
*Based on: Status Report (2026-09-05, branch `version-1.5-niranjan`) + Judging Rubric*

---

## 0. Rubric-to-Effort Map (why this order)

| Criterion | Weight | Current Score | Biggest Lever |
|---|---|---|---|
| End product quality | 30% | 6/10 | Fix split bugs (demo-breaking) |
| Novelty and impact | 20% | 8/10 | Protect it — don't let a crash undercut the pitch |
| HackTracker: creative phone use | 15% | 5/10 | Real LLM call, verified voice |
| Technical depth | 15% | 9/10 | Already won — just don't regress it |
| HackTracker: Office Kit usage | 10% | 1/10 | Any real bridge beats a stub |
| Demo and presentation | 10% | 7/10 | Rehearsal + calibration |

**Reading this:** 45% of your score (product quality + creative phone use) is sitting on three things that are currently broken or fake: **split bugs, the LLM stub, the Office Kit stub.** Technical depth is already a 9 — resist the urge to add more depth-flexing there and instead spend hours converting stubs into real, demoable behavior. Judges reward "it works" over "it's clever" at the margin you're at.

---

## 1. MUST DO (non-negotiable — demo dies without these)

These block a judge from seeing a clean run. Do these first, in this order, no exceptions.

### 1.1 Fix Split Bugs S-1, S-2, S-3, S-4, S-6 — **6–7h**
This is your single highest-leverage block. A judge who taps "Split" during the live demo currently sees a frozen UI and duplicate DB rows. That's a 30%-weight category actively failing on stage.
- **S-1** (`isPaid` resets to false) + **S-4** (`isPaid` hardcoded false in `confirmSplit()`): same root symptom — trace both writes, make `isPaid` state persist correctly through re-open and confirm.
- **S-2** (`confirmSplit()` always inserts, never updates → duplicate `SplitRecordEntity` rows): needs an existence check before insert, or an upsert.
- **S-3** (`SplitParticipantUiModel` uses mutable `var` fields, so Compose doesn't recompose → UI looks frozen): convert to immutable `data class` with `copy()` on update, or wrap in proper `State`/`StateFlow` emission.
- **S-6** (existing split record ID not stored, so `updateSplit` can't target the right row): store the ID on load, pass it through to update calls.
- Skip S-5 (`getAllSplits().filter{}` vs `getSplitsForTransaction()`) and S-7 (re-prompt on every edit) unless S-1/2/3/4/6 finish early — they're correctness/annoyance issues, not demo-crashing ones.
- **Definition of done:** create a split, mark a participant paid, back out, re-open — state holds. Do it twice in a row (simulating judge behavior) to catch duplicate-row regressions.

### 1.2 Verify/Fix Voice Bug V-2 — **2–3h**
`recognizeWithRecord()` may still be reading `recognizer.result` (intermediate) instead of `finalResult`. If so, confidence is always absent, so every voice capture silently falls back to manual entry — voice looks "supported" in code but never actually fires in the demo.
- Pull up the method, confirm which result object is used.
- If still intermediate: swap to `finalResult`, rebuild, test on the actual demo device with 5–6 real utterances (background noise on).
- **Definition of done:** speak a transaction amount out loud on-device, watch it populate without falling to manual entry, at least 4/5 times.

### 1.3 Calibrate Shake Detection on the Demo Device — **1.5–2h**
`accelThreshold = 15 m/s²`, `minReversals = 3` are untested defaults. If they're too strict for the actual demo phone in your actual hand, the flagship gesture may just not fire in front of judges.
- Log raw sensor values while doing your intended demo shake motion 10–15 times.
- Tune `accelThreshold` and `minReversals` down/up until it reliably triggers on a real, natural-feeling shake without triggering during normal phone handling (walking, picking up the phone).
- **Definition of done:** 8/10 intentional shakes fire the chip; casual handling doesn't.

### 1.4 The One Item You Specifically Asked For: Notification Listener Reliability + Shake Consistency — **2–3h**
You flagged this as a fixed must-implement regardless of what else is on this list, so it's here as its own line item, not folded into 1.3.
- **N-1 fix (dropped early notifications):** `transactionRouter = null` until `ShakeDetectionService.onCreate()` runs, and any notification arriving before that is silently dropped (`?: return`). Queue notifications that arrive before the router is set (a small in-memory buffer flushed once the router attaches) instead of discarding them.
- **N-2 mitigation (OS kills the listener on MIUI/Android 13 Doze, no auto-rebind):** implement rebind-on-`onListenerDisconnected()` — call `requestRebind()` (the standard `NotificationListenerService` API for this) instead of leaving it disconnected. This won't fully solve OEM aggressiveness but converts "silently dead for the rest of the demo" into "recovers within seconds."
- **Shake consistency:** this overlaps with 1.3's calibration — treat 1.3's tuning pass and this as one continuous effort: tune thresholds *and* confirm the debounce (2000ms gate) isn't eating legitimate repeated shakes during a live multi-transaction demo.
- **Definition of done:** kill the notification listener via adb (`adb shell cmd notification allow_listener` toggle or force-stop simulation) mid-session and confirm it recovers; do 10 shakes in a row spaced naturally and count consistent triggers.

### 1.5 Full Demo Rehearsal, 5x, on the Real Device — **1.5–2h**
Non-negotiable last block. Everything above is worthless if the live run hasn't been exercised end-to-end repeatedly on the actual hardware you'll present with.
- Run the literal demo script from `PROJECT_WALKTHROUGH.md` five full times back-to-back.
- Write down calibrated values (threshold, reversals, debounce) once they're locked, so nobody accidentally changes them again before presenting.
- Assign a fallback plan out loud with the team for each fragile step (e.g., "if shake doesn't fire in 3 seconds, tap the manual add button and narrate it as the fallback path").

**Must-do subtotal: ~13–17h**

---

## 2. SHOULD DO (only after everything in §1 is done and verified — highest score-per-hour of what's left)

### 2.1 Office Kit Bridge — Any Plausible Real Connection — **3–4h**
This category is worth 10% and you're at 1/10 with a pure stub. You don't need a full SDK integration in 30 hours — you need something a judge can point to and see actually happen, not just hear described.
- Minimum viable version: a real device-to-device signal — e.g., the phone detects a paired/nearby laptop (Bluetooth discovery, or even a simple local-network handshake like a lightweight socket/HTTP ping to a laptop-side listener you write in a few hours) and on success, **switches phrasing strategy or destination** (e.g., "report sent to Office Kit" state, or richer phrasing tier unlocked).
- The report explicitly says: *"Even a mock 'connected to laptop' state routing through a different phrasing strategy would score meaningfully higher."* That's your bar — don't over-build this. A real (not faked-in-UI-only) connection check + a visibly different resulting state is enough.
- **Definition of done:** OfficeKitBridgeEngine actually attempts a real connection (not just logs "checking"), and demo can show a before/after state change tied to that connection succeeding.

### 2.2 Real LLM Inference in OnDeviceMediaPipeEngine — **4–6h**
Currently `generatePhrasedSentences()` builds a prompt and then calls the template engine again — 0ms observed latency confirms it's not doing inference. This is squarely in the 15%-weight "creative phone use" category, and a judge who asks "show me the LLM working" currently gets template output with a straight face.
- Wire the actual MediaPipe/Gemma call using the scaffolding you already have (grounding, timeout, fallback are done — only the inference call itself is missing).
- Keep the `GroundingValidator` fallback exactly as-is: if the LLM ever produces an ungrounded number, falling back to template is a *feature*, not a bug — mention this explicitly to judges as a safety design, not a workaround.
- If this runs short on time or proves unstable close to presentation time, **stop and be upfront in the pitch** — the report itself notes this is "honest and defensible." A working template-with-grounding is safer on stage than a flaky real LLM call that stalls in front of judges.
- **Definition of done:** report generation visibly takes non-zero, plausible inference latency, and phrased output varies across runs on the same underlying numbers (proof it's not template).

### 2.3 Shake-and-Hold → Report Navigation — **1–2h**
`ShakeAndHoldEvent` already fires; it just doesn't navigate anywhere yet, only extends the capture window. This is a small, contained wire-up (event already exists, navigation graph already exists) with real demo value — "shake and hold to see the report" is a strong hook for the End Product Quality and Demo categories.
- **Definition of done:** shake-and-hold gesture takes the user to `ReportScreen` reliably.

**Should-do subtotal: ~8–12h**

---

## 3. COULD DO (only if §1 and §2 finish with real time to spare — diminishing returns)

- **S-5 / S-7 split polish** (wrong query method; re-prompting on already-split transactions) — correctness issues a sharp judge might notice on a second interaction, but won't break a first-pass demo.
- **Historical baseline seeding for projections** (currently cold-start always 0) — nice-to-have realism for the report screen if you're demoing a "fresh install" flow; skip if your demo device already has seeded transaction history.
- **Voice split participant contact resolution** (Contacts API integration for named splits) — a real feature gap, but low demo visibility relative to effort.
- **Transaction edit / History / Profile screen audits** — only worth touching if a judge is likely to click into them; otherwise, time better spent elsewhere.

Do **not** start any of these unless §1 is fully verified on-device and §2 has already yielded working, rehearsed results. If you're deciding between "start something from §3" and "run the demo rehearsal one more time," always pick the rehearsal.

---

## 4. WHAT YOU CANNOT DO IN 30 HOURS — Say So, Don't Fake It

Be explicit with the team about what's out of scope so nobody burns hours chasing it or improvises a risky change under time pressure right before presenting.

- **Full real-device validation of Whisper STT accuracy in noisy environments** — you can verify V-2 is *functionally* fixed (1.2), but a genuine accuracy study across accents/noise conditions is not a 30-hour task. State the fix, don't overclaim the coverage.
- **A production-grade Office Kit SDK integration** — 2.1's plausible-connection demo is the realistic ceiling; a real cross-device SDK partnership integration is out of scope.
- **Comprehensive instrumented/UI test coverage** — you have 175 passing JVM unit tests and zero instrumented tests; writing a real UI test suite now has no payoff versus manual rehearsal (1.5) for a live demo.
- **OEM-proof background service survival** (MIUI/OriginOS aggressive Doze killing the notification listener) — 1.4's rebind mitigation helps, but there's no 30-hour fix for OEM battery management; plan your live demo to minimize idle time between shake and notification arrival instead.
- **Real thermal-receipt OCR accuracy validation** — same story as voice; functionally works per the report, but a rigorous accuracy pass across real faded/handwritten receipts isn't happening in this window. If OCR is demoed, use a clean, high-contrast receipt.

**If a judge asks about any of these:** the report's own framing is your best answer — be upfront about what's a grounded template vs. a full LLM call, what's tuned-on-device vs. shipped-as-default, and what's architecturally ready vs. fully validated. Judges consistently score honesty about scope higher than an unraveling claim.

---

## 5. Hour-by-Hour Skeleton (30h, adjust to your actual start time)

| Block | Hours | Task |
|---|---|---|
| 1 | 0–7 | Split bug fixes (S-1/S-2/S-3/S-4/S-6) |
| 2 | 7–10 | Voice V-2 verification/fix on real device |
| 3 | 10–13 | Shake calibration + notification listener reliability (N-1, N-2) — your fixed must-do item |
| 4 | 13–14.5 | Buffer / catch-up on any §1 overrun (§1 tasks routinely run long — protect this) |
| 5 | 14.5–18.5 | Office Kit plausible bridge |
| 6 | 18.5–24 | Real LLM inference wiring (cut early if unstable — see 2.2 fallback note) |
| 7 | 24–26 | Shake-and-hold → Report navigation |
| 8 | 26–28 | Full demo rehearsal x5, lock calibrated values |
| 9 | 28–30 | Slack / sleep-in-shifts buffer / final rehearsal pass |

Treat Block 4 as sacred — §1 tasks (especially split bugs) have a track record of running longer than estimated once you're actually in the code. If Block 1 finishes early, that time rolls forward into §2, not into starting §3 items.

---

## 6. One-Line Team Reminders

- Split bugs first, always — nothing in §2 or §3 matters if the demo freezes on a tap.
- A real, honest 5/10 beats a fake 8/10 a judge can poke at — this applies directly to the LLM and Office Kit stubs.
- Lock calibration values once tuned and don't touch them again before presenting.
- Rehearse on the actual device you're presenting with, not the dev/emulator environment.

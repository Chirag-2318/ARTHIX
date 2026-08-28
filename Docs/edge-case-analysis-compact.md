# Shake & Audit — Edge Case & Risk Analysis (compact)

Companion to shake-audit-project-brief.md. Systematic pass over every FR, looking for ways the system logs wrong/missing/duplicate financial data or fails live on stage. Rubric: quality-would-they-keep-using-it 30%, technical robustness 15%.

Severity: **Critical**=corrupts data or breaks demo, fix before presenting · **High**=noticeably hurts reliability/trust · **Medium**=real but lower freq/impact · **Low**=polish. "No fix" = fundamental limit, listed in §Limits.

Format per row: `ID | Sev | Issue → Fix`

## A. FR-1 Shake-to-Log
- EC-01 High: Ordinary handling (walking/pocket/vehicle/haptics) triggers false shake → chip pops for no payment, becomes phantom pending. Fix: require oscillation (≥2 direction reversals in window), not single spike; test walking/pocket/vehicle as separate negative cases.
- EC-02 High: Weak/rushed one-handed shake missed (fits persona: queue, meeting). Fix: inherent precision/recall tradeoff (see Limits); make FR-5 manual fallback always one tap away.
- EC-03 Medium: Shake vs shake-and-hold indistinguishable at gesture start → spurious pending capture if reclassified late. Fix: single state machine — on motion start wait; if stops before hold threshold commit as FR-1, else cancel FR-1 side effects and switch to FR-7.
- EC-04 High: "Display chip UI" unspecified mechanism; user is inside UPI app, not this app. Fix: heads-up notification w/ inline action buttons, not SYSTEM_ALERT_WINDOW overlay.
- EC-05 Medium: Continuous sampling costs battery; Doze throttles sensors. Fix: SENSOR_DELAY_NORMAL when screen off; foreground service w/ low-priority persistent notification during active use (ties to EC-59).

## B. FR-2 Notification Capture
- EC-06 **Critical**: One payment → 2 notifications (UPI app + bank) both parsed as outflow = duplicate log. Fix: before committing, check amount+close payee match within few seconds; treat 2nd as confirmation not new record.
- EC-07 **Critical**: Declined/pending/refund notifications parsed as successful outflow (only inflow/outflow direction is checked). Fix: keyword-filter (declined/failed/pending/cancelled/reversed) before success path; net refunds against original txn by payee+amount rather than logging as unrelated inflow.
- EC-08 **Critical**: Text-only parsing has no source-app check → any app posting "₹123" text can be misread as payment (spoofable). Fix: check StatusBarNotification.getPackageName() against UPI app allow-list before parsing text at all.
- EC-09 High, no full fix: UPI apps change notification wording without notice → patterns silently stop matching. Mitigation: patterns in editable config; low-confidence → route to manual chip instead of silent drop.
- EC-10 Medium: Notification text can be truncated (collapsed vs expanded style). Fix: read EXTRA_BIG_TEXT/EXTRA_TEXT_LINES, not just EXTRA_TEXT; treat mid-word/no-amount matches as low-confidence.
- EC-11 Medium: Currency format varies (₹/Rs./INR, separators). Fix: single shared normalize-then-parse amount utility.
- EC-12 Medium: Android may bundle rapid notifications into one summary, breaking 1-notif-per-payment assumption. Fix: inspect grouped notification child entries/inbox-style lines.
- EC-13 Low: NotificationListenerService binding lag after grant/restart → payments in that window missed. Fix: show "reconnecting…" state on app start.

## C. FR-2a Event Reconciliation Engine
(Design is sound overall — nearest-neighbor matching, independent timeouts, disambiguation path are right calls; gaps below.)
- EC-14 **Critical**: "Ambiguous" undefined ("not clearly correct" isn't codeable). Fix: score-gap rule — if time-delta gap between best & 2nd-best match is under a threshold, flag ambiguous; tune threshold from Phase 5 data, define before Phase 1.
- EC-15 Medium: No fallback if disambiguation prompt ignored. Fix: timeout → auto-pick nearest-neighbor best guess but flag "auto-resolved — tap to review"; never wait indefinitely or default silently.
- EC-16 **Critical**: No way to discard a false-positive shake; every unmatched shake becomes permanent ghost entry (FR-3 only offers category mapping). Fix: add "not a transaction" discard action in both chip UI and voice flow — one of the most important fixes in the doc.
- EC-17 High: Timed-out queue entries must be actively removed from the matching pool, not just marked pending, or a later unrelated notification can match a stale entry. Fix: remove from active queue at the same moment as timeout.
- EC-18 **Critical**: Shake events (SensorEventListener thread) and notification events (binder thread) both mutate same PendingCaptures/PendingNotifications queues unsynchronized = race condition. Fix: route both through one single-threaded processing queue, or use a properly locked structure. Correctness issue, fix before optimizing.
- EC-19 Medium: Wall-clock time (currentTimeMillis) for match math is vulnerable to clock/timezone shifts mid-session. Fix: use SystemClock.elapsedRealtime() (monotonic) for matching; keep wall-clock only for display.
- EC-20 Medium: Burst of 4–5 payments in seconds (group bill) stresses nearest-neighbor matching, esp. if EC-14 threshold too narrow. Fix: add explicit 4–5-txn burst as its own Phase 5 test case.
- EC-21 Low: Shake coinciding with an inflow (which bypasses capture queue) becomes an orphan pending entry. Fix: same discard mechanism as EC-16.
- EC-22 High: Late-arriving notification (up to 2 min) silently enriches an already-tapped chip with no confirmation; wrong match goes unnoticed. Fix: flag low-confidence/ambiguous-resolved enrichments visibly in review/history (ties to EC-52 edit feature).
- EC-23 High: No link from committed Transaction back to its source PendingCapture/Notification — blocks future debugging/edit/undo. Fix: store source capture/notification ID on the Transaction record at match time.

## D. FR-3 Batched Voice Follow-Up
- EC-24 **Critical**: Voice flow only ever resolves category, never amount — but a capture that timed out with no notification match has no amount at all, so record stays permanently incomplete. Fix: if pending record lacks amount, ask for it too ("how much did you spend at the last one?").
- EC-25 **Critical**: No "this didn't happen" option in voice flow (same gap as EC-16, needs voice-recognizable phrasing: "skip"/"not real"/"ignore that one"). Fix: add discard-intent recognition to voice mapping.
- EC-26 High: Idle-detection ("low accelerometer variance for N min") can fire while phone is just charging/unattended, prompting into an empty room. Fix: require recent screen unlock + check Do Not Disturb/ringer state before firing audio prompt.
- EC-27 Medium: No fallback specified for low STT confidence — risk of silently mapping garbled speech to wrong category. Fix: re-prompt once on low confidence; on repeat failure, fall back to tap-based chip UI.
- EC-28 Medium: Free-text categories outside fixed set fragment the taxonomy over time. Fix: fuzzy-normalize to nearest existing category; store original phrase as a sub-tag.
- EC-29 Medium: On-device STT language coverage unscoped; persona likely code-switches Hindi/English. Fix: explicitly define supported language(s); test code-switched phrases before Phase 6.

## E. FR-4 Camera OCR Logging
- EC-30 High: Receipt has subtotal/tax/tip/total — no rule for which number is "the amount." Fix: match "Total/Grand Total/Amount Payable" keyword proximity first; fallback to largest number only as low-confidence.
- EC-31 Medium: Faded thermal paper / handwritten receipts (common for target persona's small vendors) — no defined behavior when OCR can't extract a confident amount. Fix: fall back to manual chip UI pre-filled with any partial data extracted, not silent failure or blank re-entry.
- EC-32 High: ₹ symbol / decimal misreads can shift amount by orders of magnitude. Fix: sanity-range-check extracted amounts (e.g. <₹1 or >₹50,000 → require manual confirmation).
- EC-33 Low: Small vendors' receipts often have no vendor name. Fix: leave payee blank, let user fill via pre-fill confirmation chip rather than inserting garbage OCR text.
- EC-34 Low: No OCR-specific latency target (only report generation has one) — risk of a visible on-stage hang. Fix: set an explicit few-second OCR latency target, test on demo device.

## F. FR-5 Manual Chip/Text Fallback
- EC-35 Medium: FR-5 is the safety net for nearly every other failure mode (EC-02, EC-31, EC-27) but is only specified as "a manual trigger" with no discoverability/speed requirement. Fix: make reachability (persistent quick-access button/widget) a first-class requirement.

## G. FR-6 Splitwise-Style Split
- EC-36 Medium: Ambiguous contact-name match (e.g. two "Aman"s) not addressed (only unrecognized names are handled). Fix: surface both matches as tap-to-pick, don't silently guess.
- EC-37 Low: Non-contact/one-off participants not addressed. Fix: allow ad hoc name entry into a split group.
- EC-38 Medium: Non-divisible even splits leave a rounding remainder (₹100/3). Fix: explicit rule — payer absorbs the remainder.
- EC-39 Low: Voice splits likely support even-split only, no stated scope for proportional splits via voice. Fix: document as intentional; uneven splits require tap UI.
- EC-40 High: No rule for whether a SplitRecord recalculates or stays locked when its underlying transaction amount is later corrected. Fix: pick one explicit rule (auto-recalculate vs lock-and-reconfirm).
- EC-41 Medium: Cold-start (no history) breaks category+time-pattern group suggestion. Fix: explicit no-suggestion default for cold start, prompt manual pick.
- EC-42 High: Wrong split (mis-transcribed voice, bad auto-suggestion) causes real-world friction between people even though no money moves in-app. Fix: never auto-commit a voice-parsed split — always show tap-to-confirm summary first.

## H. FR-7 AI Spending Agent / Report
- EC-43 **Critical**: Naive (spend/time-elapsed)×period extrapolation explodes on thin/early data (e.g. 2 txns in 2 min → absurd projection). Fix: anchor projection to Phase-6 pre-seeded historical baseline, blend with recent rate rather than raw extrapolation.
- EC-44 High: Unclear whether pending/unlabeled transactions count toward report totals; silent exclusion undercounts spend. Fix: include pending amounts in overall total (never invisible), exclude only from category-specific suggestions until resolved.
- EC-45 Medium: Zero-transaction previous period → divide-by-zero in percentage comparison. Fix: explicitly handle zero-baseline as "no prior data," not NaN/Infinity%.
- EC-46 Medium: Floating-point currency arithmetic drifts over many transactions. Fix: store amounts as integer paise, convert to rupees only for display.
- EC-47 **Critical**: Office Kit bridge is a single point of failure for report generation, contradicting the doc's own no-laptop-dependency constraint and live on-device demo criterion. Fix: build on-device-only path as default; treat bridge as an enhancement layer, not a dependency.
- EC-48 High: If the LLM both computes and phrases the suggestion, risk of hallucinated figures not matching real data. Fix: compute sums/thresholds/projections deterministically in code; model only phrases already-verified numbers.
- EC-49 Medium: On-device LLM latency risk vs 15s budget (NFR-4). Fix: same deterministic-precompute approach as EC-48 (model only phrases, doesn't compute) + favor small/quantized model.
- EC-50 Low: "Current vs previous period" semantics undefined for daily vs weekly triggers. Fix: define explicitly (e.g. always week-over-week + separate "today so far" line).

## I. Storage Layer & Data Model
- EC-51 **Critical**: PendingCaptures/PendingNotifications likely in-memory only (not in §2.4 data model) → app kill/reboot silently discards in-flight reconciliation state. Fix: persist both pending queues, not just committed transactions.
- EC-52 **Critical**: No edit/delete/correct affordance anywhere across all 7 FRs, despite every capture path being able to produce a wrong value. Fix: add a basic edit/delete screen for committed transactions.
- EC-53 High: `pending: bool` can't represent the system's actual states (awaiting match / category / amount / discarded). Fix: replace with status enum (confirmed | awaiting_match | awaiting_category | awaiting_amount | discarded).
- EC-54 Medium: Local DB write atomicity unspecified — crash mid-write could corrupt a money record if using a flat file. Fix: use a real embedded DB (Room/SQLite, WAL mode) for atomic writes.
- EC-55 Low, no fix needed: Local-only storage = total data loss on uninstall/device change. This is an accepted, in-scope limitation (§2.5), not a bug — note it in pitch/README if asked.

## J. Security & Privacy
- EC-56 **Critical**: NotificationListenerService can technically read all notifications (OTPs, messages), not just UPI, though intent is scoped to UPI. Fix: same allow-list as EC-08 as a hard rule — never log/cache/retain non-allow-listed content even transiently.
- EC-57 Medium: No at-rest encryption specified for local store (payee names, amounts, timestamps = spending fingerprint). Fix: basic at-rest encryption (SQLCipher / Keystore-backed) if time allows; else flag consciously as a known gap.
- EC-58 Low: Android's notification-listener permission dialog warns it can read "all notifications, including passwords" — real first-run friction. Fix: explain the "why" in your own onboarding screen before the system dialog appears.

## K. Android Platform / OEM (OriginOS)
- EC-59 **Critical**, no full fix: OriginOS (vivo/iQOO) is known to aggressively kill background services/throttle sensors even with correct permissions — threatens both FR-1 and FR-2 whenever app isn't foreground (most of the time). Mitigation: prompt battery-optimization whitelist during onboarding; foreground service w/ persistent low-priority notification; ensure every silent miss degrades gracefully to manual fallback (FR-5).
- EC-60 High: Many special permissions (notification listener, mic, camera, contacts, battery whitelist) — one missing/revoked permission silently breaks a path on the demo device. Fix: explicit permission-state verification as its own item on the pre-demo checklist (Phase 6), checked right before going on stage.

## L. Demo-Day / Live Presentation Risks
- EC-61 High: Simulated-notification fallback (Phase 1 exit criteria allows it) must be as reliable as the real path, or you lose your insurance policy along with the primary path if it has bugs. Fix: rehearse the simulated path exactly as much as the real one.
- EC-62 Medium: Judges (robustness = 15% of rubric) are likely to probe concurrent payments, ambiguous matches, false positives directly. Fix: deliberately demo one edge case live (e.g. trigger disambiguation on purpose) rather than only handling it invisibly.

## Fundamental limits — no complete fix exists
1. Shake-detection precision vs recall (EC-01/02): a tight threshold misses gentle shakes, a loose one catches false positives — inherent tradeoff. Mitigate via cheap correction path, not perfect tuning.
2. OEM background execution (EC-59): no app code can guarantee OriginOS won't kill a background service. Only reduce odds + degrade gracefully.
3. Truly indistinguishable ambiguous matches: near-identical amount/timing/payee clusters carry no more signal than a human glance — disambiguation prompt is the ceiling, no algorithmic resolution exists.
4. UPI notification format drift (EC-09): third-party apps can change wording anytime with no warning — ongoing maintenance, not a one-time fix.
5. LLM-phrased suggestion text (EC-48): even with grounded numbers, freely-generated phrasing carries residual wording risk; zero risk only via full rigid templating (trades away personalization). Conscious quality-vs-safety call.
6. No cloud backup (EC-55): local-only storage means lost/replaced phone = lost history — deliberately excluded per §2.5, not solvable without sync.

## Recommended PRD additions (punch list)
1. Edit/delete transaction requirement (EC-52).
2. "Not a transaction" discard path, from chip UI and voice (EC-16, EC-25).
3. Extend FR-3 to resolve amount, not just category (EC-24).
4. Package-name allow-listing in FR-2 (EC-08, EC-56).
5. Handle declined/failed/pending/refund notification states (EC-07).
6. Replace `pending: bool` with a status enum (EC-53).
7. Persist PendingCaptures/PendingNotifications (EC-51).
8. Numeric ambiguity threshold for FR-2a (EC-14).
9. Disambiguation prompt timeout/fallback behavior (EC-15).
10. On-device-only fallback path for FR-7 (EC-47).
11. Ground FR-7 suggestion text in pre-computed numbers; model only phrases (EC-48).
12. Handle zero-baseline previous period in report math (EC-45).
13. Store amounts as integer paise (EC-46).
14. "Needs review"/confidence flag for auto-inferred fields — covers EC-22, EC-27, EC-30, EC-32.
15. Define "which number is the total" OCR heuristic (EC-30).
16. Define contact-name disambiguation UX for FR-6 (EC-36).
17. Decide whether SplitRecord amounts recalculate or lock on correction (EC-40).

Highest-leverage next steps: reconciliation engine's matching algorithm (§C) and the notification-parser allow-list (§B) — most other sections depend on these or get safer once they're solid.

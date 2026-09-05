# ARTHIX — AI Goal Planner (On-Device Savings Plan + Progress Tracking)
### Feature Design Doc

**Status:** Proposed
**Positioning:** A goal-based savings planner computed entirely from local UPI/SMS spending history — no bank account linking, no balance visibility, no credentials shared. This is the key differentiator versus existing goal-planning features in apps like Jupiter, Plum, Fisdom, and jUMPP, all of which require bank-account or balance-level access to compute a plan.

---

## 1. What This Feature Does

1. User states a goal: an item and a target price (e.g., "mouse, ₹2000").
2. ARTHIX analyzes the user's existing categorized spending history (data already captured by the reconciliation + categorization engine — no new permission needed).
3. It generates a simple plan: either a category-based suggestion ("cut X category spending to save ₹Y over Z days") or a flat weekly/daily set-aside amount if spending history is too thin for a category-based suggestion.
4. A **progress bar** tracks how much of the goal has been "saved" so far, updated automatically as the plan proceeds.
5. Progress ties into the existing streak/gamification system rather than introducing a new one.

## 2. Why On-Device Is the Differentiator

Every comparable goal-planning feature found in current apps depends on account-level access:

- Jupiter's goal-setting ("Pots") and Plum's automatic savings both require a connected/linked account to know actual balance and move money.
- Fisdom's SmartBudget adjusts savings/investment amounts based on spending habits, but as part of a platform that already has investment-account visibility.
- Fi's "Ask Fi" and jUMPP's assistant answer goal-progress questions, but both operate within apps that already have full account/transaction visibility via bank linking.

ARTHIX doesn't need account balance to produce a useful plan — spending *rate* and *category breakdown*, which the app already computes, is enough to generate a meaningful recommendation. No new sensitive permission, no bank login, nothing that conflicts with the existing on-device-only privacy stance.

## 3. Plan Generation Logic (Kept Simple)

```
User enters goal: item name + target amount
        │
        ▼
Look at existing categorized transaction history
(already computed by ReconciliationEngine + CategorySuggestionEngine)
        │
        ▼
Enough history in a specific category to spot a reducible pattern?
        │
   ┌────┴────┐
  Yes         No (thin history / new user)
   │           │
   ▼           ▹
Category-based           Flat suggestion:
suggestion:               target amount ÷ a
"Reduce [category]        reasonable timeframe
spend by ₹X/week          (e.g. 8-12 weeks)
to reach goal in          = suggested weekly
Y days"                   set-aside amount
```

- No forecasting model, no external API, no ML infra required to start — a rule-based comparison against the user's own recent average per category is enough for a first version, and is honest about being an estimate rather than a guarantee.
- The plan is a **suggestion**, not an automated transfer — ARTHIX doesn't move money or restrict spending; it only tracks and nudges.

## 4. Progress Tracking

- Each goal gets a simple progress bar: **amount saved so far / target amount.**
- "Amount saved" is calculated by comparing actual spending in the relevant category (or overall, for a flat-plan goal) against the baseline average used to generate the plan — i.e., money not spent relative to the pre-goal baseline counts as progress toward the goal.
- Progress updates automatically as new transactions are categorized — no manual entry needed, consistent with the rest of the app's zero-manual-effort principle.
- Multiple concurrent goals are supported, each with its own independent progress bar.
- Reuses the existing streak/gamification component for visual presentation and any "on track" / "off track" messaging, rather than building a separate progress system from scratch.

## 5. UI Touchpoints

- **New goal creation:** simple form — item name (optional, for context) + target amount. Reuses existing input components.
- **Goal card:** shows target amount, progress bar, and current plan (e.g. "Cut Food delivery by ₹150/week" or "Save ₹200/week"). Lives alongside existing dashboard elements, not a new screen paradigm.
- **Completion state:** when progress reaches 100%, a simple celebratory state (ties into the existing streak/gamification visuals) — no new animation system needed if one already exists for streaks.

## 6. What This Reuses (No New Subsystems)

- Categorized transaction data (`ReconciliationEngine`, `CategorySuggestionEngine`)
- Existing streak/gamification component, for progress visuals and messaging
- Existing encrypted local storage layer, for goal + progress state
- Existing dashboard UI shell, for goal cards

## 7. What This Explicitly Avoids

- No bank account linking or balance access of any kind
- No automated fund transfers or spending restrictions — purely a tracking/suggestion tool
- No external AI API calls with financial data — plan generation is rule-based on local averages, keeping the feature consistent with the app's on-device-only commitment
- No new permissions beyond what ARTHIX already requests

## 8. Known Limitation to Flag Honestly

A rule-based average-comparison plan is a reasonable estimate, not a guarantee — actual behavior change (does the user really spend less?) can't be verified without knowing real account balance. This should be presented to the user as a **suggested pace**, not a certainty, to avoid overpromising what an account-balance-free system can actually confirm.

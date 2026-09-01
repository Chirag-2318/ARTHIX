# Voice STT Fix Plan — ARTHIX

## Problem Statement

The VOSK-based voice recognition (`VoskSttEngine`) is not recognizing commands when users speak. After analyzing the full voice pipeline, the following root causes have been identified.

---

## Root Cause Analysis

### Bug 1 — Silence Detection Kills Utterances Too Early (CRITICAL)

**File:** `VoskSttEngine.kt` — `recognizeWithRecord()`, lines 158–160

```kotlin
val isSilent = buffer.take(read).all { it.toInt() in -300..300 }
if (isSilent) silenceFrames++ else silenceFrames = 0
if (silenceFrames >= maxSilenceFrames) break
```

**Problem:** The silence threshold of `±300` is dangerously low. Background noise, microphone gain variance, and Indian-English speech patterns (which have natural pauses) frequently produce samples in this range — especially at the **start** of an utterance while the speaker draws breath. The recognizer exits before any speech has been processed.

`maxSilenceFrames = 20` at ~0.125 seconds per frame = **~2.5 seconds**. But there is no minimum recording guard, so the loop can exit after 20 frames of initial silence — before the user even begins speaking.

**Fix:** Raise silence threshold to `±800`, increase `maxSilenceFrames` to `40` (~5 seconds), and add a **minimum frame guard** of 8 frames (~1 second) so silence cannot trigger in the first second.

---

### Bug 2 — `acceptWaveForm` Intermediate Result Lacks `conf` Field (CRITICAL)

**File:** `VoskSttEngine.kt` — `recognizeWithRecord()`, lines 162–165

```kotlin
if (recognizer.acceptWaveForm(buffer, read)) {
    return parseResult(recognizer.result)   // <-- WRONG: uses non-final result
}
```

**Problem:** VOSK's `acceptWaveForm` returns `true` on internal sentence boundaries. This calls `recognizer.result` (non-final JSON), which **never contains a `conf` field** — only `finalResult` does. This means:

- `confidence = obj.optDouble("conf", -1.0).toFloat()` → always returns `-1.0`
- `-1.0` is NOT in `0f..1f`, so the `if (confidence in 0f..1f && confidence < CONFIDENCE_THRESHOLD)` check is skipped
- It falls through to `SttResult.Recognized(text, (-1.0f).coerceAtLeast(0f))` → `confidence = 0.0`

Wait — actually re-reading the code: `coerceAtLeast(0f)` on `(-1.0f)` gives `0.0f`. And the check `confidence in 0f..1f && confidence < 0.5f` means `0.0 < 0.5` → **`SttResult.LowConfidence`**. So every single intermediate result, no matter what was said, is `LowConfidence` → re-prompt → still `LowConfidence` → manual fallback. Voice never works.

**Fix:** **Never use `recognizer.result`** (intermediate). Always drive to completion and use `recognizer.finalResult`.

---

### Bug 3 — Confidence Threshold 0.5 Too High for Small Model (MEDIUM)

**File:** `VoskSttEngine.kt` — companion object

```kotlin
const val CONFIDENCE_THRESHOLD = 0.5f
```

**Problem:** `vosk-model-small-en-in-0.4` is a small 37MB model. Small VOSK models routinely produce `conf` in the `0.2–0.45` range for correctly recognized speech. Even after fixing Bug 2, a threshold of `0.5` will reject half of valid utterances.

**Fix:** Lower to `0.3f`.

---

### Bug 4 — Missing Confidence Treated as Low Confidence (LOW)

**File:** `VoskSttEngine.kt` — `parseResult()`

If `conf` field is absent from the JSON (VOSK does this when the model is uncertain), `optDouble("conf", -1.0)` returns `-1.0`. After `coerceAtLeast(0f)` → `0.0f`, which is `< 0.5f` → `LowConfidence`. This is a silent failure.

**Fix:** Treat `conf = -1.0` (field absent) as passing the threshold — don't penalize absent confidence.

---

## Summary of Bugs

| # | Severity | Description | Impact |
|---|----------|-------------|--------|
| 1 | CRITICAL | Silence threshold ±300 too tight, exits before speech | Vosk never hears you |
| 2 | CRITICAL | `recognizer.result` has no `conf` → always LowConfidence → always manual fallback | Nothing recognized |
| 3 | MEDIUM   | Threshold 0.5 too high for small model | Excess fallbacks even when fixed |
| 4 | LOW      | Missing conf treated as 0.0 (LowConfidence) instead of valid | Intermittent failures |

---

## Proposed Changes — Single File: `VoskSttEngine.kt`

No UI changes. No other files.

### 1. Update constants in `companion object`

```diff
-const val CONFIDENCE_THRESHOLD = 0.5f
+const val CONFIDENCE_THRESHOLD = 0.3f      // small model scores 0.2-0.45 for valid speech

 private const val SAMPLE_RATE = 16000
 private const val RECORD_TIMEOUT_MS = 8_000L
 private const val BUFFER_SIZE_FACTOR = 4
+private const val SILENCE_THRESHOLD = 800  // was 300 — too tight, triggers on breath/noise
+private const val MAX_SILENCE_FRAMES = 40  // was 20 — now ~5s instead of ~2.5s
+private const val MIN_RECORD_FRAMES = 8    // NEW — don't check silence in first ~1s
```

### 2. Rewrite `recognizeWithRecord()` — Fix Bug 1 + Bug 2

```diff
 private fun recognizeWithRecord(
     audioRecord: AudioRecord,
     recognizer: Recognizer,
     bufferSize: Int,
 ): SttResult {
     audioRecord.startRecording()
     val buffer = ShortArray(bufferSize / 2)
     var silenceFrames = 0
-    val maxSilenceFrames = 20  // ~2.5s of silence ends recording
+    var totalFrames = 0

     while (true) {
         val read = audioRecord.read(buffer, 0, buffer.size)
         if (read <= 0) break

-        val isSilent = buffer.take(read).all { it.toInt() in -300..300 }
-        if (isSilent) silenceFrames++ else silenceFrames = 0
-        if (silenceFrames >= maxSilenceFrames) break
+        totalFrames++
+        val isSilent = buffer.take(read).all { it.toInt() in -SILENCE_THRESHOLD..SILENCE_THRESHOLD }
+        if (isSilent && totalFrames >= MIN_RECORD_FRAMES) silenceFrames++ else silenceFrames = 0
+        if (silenceFrames >= MAX_SILENCE_FRAMES) break

-        if (recognizer.acceptWaveForm(buffer, read)) {
-            // Final result available — parse it
-            return parseResult(recognizer.result)
-        }
+        // Feed to recognizer — do NOT return on intermediate result.
+        // recognizer.result (non-final) never contains 'conf' field.
+        // Always use finalResult below for correct confidence scoring.
+        recognizer.acceptWaveForm(buffer, read)
     }

-    // End of audio — get final result
     return parseResult(recognizer.finalResult)
 }
```

### 3. Update `parseResult()` — Fix Bug 4

```diff
     val text = obj.optString("text", "").trim().lowercase()
     val confidence = obj.optDouble("conf", -1.0).toFloat()

     Log.d(TAG, "Vosk result: text='$text' conf=$confidence")

     if (text.isEmpty()) return SttResult.Timeout

-    if (confidence in 0f..1f && confidence < CONFIDENCE_THRESHOLD) {
+    // If conf field is absent (returns -1.0), treat as passing threshold
+    val effectiveConf = if (confidence < 0f) CONFIDENCE_THRESHOLD else confidence
+    if (effectiveConf < CONFIDENCE_THRESHOLD) {
         SttResult.LowConfidence(text, confidence)
     } else {
         SttResult.Recognized(text, confidence.coerceAtLeast(0f))
     }
```

---

## Files Modified

### [MODIFY] [`VoskSttEngine.kt`](file:///c:/Users/Niru/Documents/coding/ARTHIX/app/src/main/java/com/chirag/arthix/voice/VoskSttEngine.kt)

- `companion object`: Lower `CONFIDENCE_THRESHOLD` to `0.3f`, add `SILENCE_THRESHOLD`, `MAX_SILENCE_FRAMES`, `MIN_RECORD_FRAMES`
- `recognizeWithRecord()`: Remove early return on `acceptWaveForm`, fix silence detection, add min-frame guard
- `parseResult()`: Treat absent `conf` field as passing threshold

### No other files are changed

- `VoiceCaptureBottomSheet.kt` — no change
- `VoiceFollowUpSession.kt` — no change
- `VoiceIntentParser.kt` — no change
- `build.gradle.kts` — no change (VOSK library stays)
- `AndroidManifest.xml` — no change
- Assets — no change (existing model zip stays)

---

## Why Not Replace VOSK with Another Model?

The model is not the problem. The bugs are entirely in the audio loop and result parsing logic:

- `vosk-model-small-en-in-0.4` is the correct choice: offline, Indian-English tuned, 37MB
- All VOSK models (large or small) have the same `acceptWaveForm` / `finalResult` behavior
- Switching models would NOT fix Bug 1 or Bug 2, would increase APK size, and would break the offline NFR

---

## Verification Plan

### Manual test after installing APK

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

1. Tap mic button → Voice Quick Log sheet opens
2. Say **"three hundred food"** → expect: `Recognized: "three hundred food"`, amount=300, category=food
3. Say **"fifty cab"** → expect: amount=50, category=travel  
4. Say **"skip"** → expect: discard intent
5. Check Logcat tag `VoskSttEngine` — `conf` should now be a positive float (e.g., `0.35`), not `-1.0`

### Unit tests (must still pass)

```powershell
.\gradlew.bat testDebugUnitTest
```

All 175 tests must continue to pass — the changes are in hardware-dependent audio code, not in the pure-Kotlin intent parsing logic that is unit-tested.

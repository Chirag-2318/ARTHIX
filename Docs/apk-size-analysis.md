# ARTHIX APK Size & Storage Distribution Analysis

> **Analysis Date:** September 2026  
> **App Version:** 1.0.0 (Debug Build: `app-debug.apk`)  
> **Total Binary Size:** ~302 – 305 MB  

---

## 1. High-Level Storage Distribution

| Component | Category | Compressed Size (in APK) | % of Total | Core Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **`lib/`** | Native C++ Libraries | **171.36 MB** | **56.5%** | Precompiled `.so` native inference and crypto engines across 4 CPU architectures |
| **`assets/`** | Offline ML Models & Data | **60.74 MB** | **20.0%** | On-device Whisper voice transcription model & ML Kit OCR detection models |
| **`res/`** | Drawables & UI Assets | **47.83 MB** | **15.8%** | High-resolution uncompressed PNG graphics (onboarding & account screens) |
| **`classes*.dex`** | Compiled Bytecode | **21.72 MB** (72 MB raw) | **7.2%** | Unshrunk application, Jetpack Compose, ML Kit, Hilt & dependency classes |
| **`resources.arsc` & Meta** | Metadata & Resources Table | **1.45 MB** | **0.5%** | Compiled resource tables, XML layouts, and package manifests |
| **Total** | | **~303.1 MB** | **100%** | |

---

## 2. Deep-Dive Breakdown

### A. Native C++ Libraries (`lib/` — 171.36 MB)
The primary driver of the ~300 MB footprint is bundling native binaries for **all four Android ABIs (CPU architectures)** into a single universal APK.

#### Distribution by Architecture:
* **`x86_64` (64-bit Emulator):** 49.83 MB
* **`x86` (32-bit Emulator):** 49.16 MB
* **`arm64-v8a` (Modern Android Phones):** 43.10 MB
* **`armeabi-v7a` (Older 32-bit Phones):** 29.27 MB

#### Heaviest Native Binaries Across All Architectures:
| Library (`.so`) | Package / Feature | Total Size Across ABIs | Size on Modern Phone (`arm64-v8a`) |
| :--- | :--- | :--- | :--- |
| **`libonnxruntime.so`** | Sherpa-ONNX / ONNX Runtime engine (STT) | **~101.6 MB** | **24.63 MB** |
| **`libmlkit_google_ocr_pipeline.so`** | Google ML Kit on-device Text Recognition (Bill OCR) | **~39.1 MB** | **10.55 MB** |
| **`libsherpa-onnx-jni.so`** | Sherpa-ONNX JNI Native Interface | **~17.7 MB** | **4.41 MB** |
| **`libsqlcipher.so`** | SQLCipher 256-bit database encryption | **~12.8 MB** | **3.46 MB** |
| **`libimage_processing_util_jni.so`**| ML Kit image utilities | **~0.12 MB** | **0.03 MB** |

> **Critical Insight:** A physical Android smartphone only requires **`arm64-v8a` (~43.1 MB)**. Bundling `x86`, `x86_64`, and `armeabi-v7a` introduces **~128 MB of completely unused binary data** on end-user devices.

---

### B. Bundled Assets & ML Models (`assets/` — 60.74 MB)
Located in `app/src/main/assets/`:

1. **`whisper-tiny-model.zip` (59.51 MB):**
   * Pre-packaged offline Kaldi/Zipformer/Whisper model for on-device speech-to-text.
   * Marked as `noCompress += "zip"` in `app/build.gradle.kts`, meaning it is packaged uncompressed into the APK for instant extraction at runtime.
2. **`mlkit-google-ocr-models/` (~1.23 MB):**
   * On-device TFLite models (`rpn_text_detector`, `lstm_model.fb`, `tflite_langid.tflite`).

---

### C. Drawables & Image Artwork (`res/drawable/` — 47.83 MB)
Located in `app/src/main/res/drawable/`:

The app bundles large, uncompressed high-resolution PNG files directly into resources:

| File | Size in APK | Referenced Screen | Status |
| :--- | :--- | :--- | :--- |
| `ac.png` | **8.17 MB** | `CreateAccountScreen.kt` | Active |
| `n2.png` | **5.46 MB** | `OnboardingScreen.kt` | Active |
| `n4.png` | **5.24 MB** | `OnboardingScreen.kt` | Active |
| `n3.png` | **5.20 MB** | `OnboardingScreen.kt` | Active |
| `n5.png` | **5.17 MB** | `OnboardingScreen.kt` | Active |
| `n1.png` | **5.10 MB** | `OnboardingScreen.kt` | Active |
| `n6.png` | **4.97 MB** | `OnboardingScreen.kt` | Active |
| `g.png` | **1.17 MB** | *None (Unreferenced)* | Unused |
| `j.png` | **1.11 MB** | *None (Unreferenced)* | Unused |
| `i.png` | **0.74 MB** | *None (Unreferenced)* | Unused |
| `e.png` | **0.68 MB** | *None (Unreferenced)* | Unused |
| `ill_manual_entry.webp` | **0.66 MB** | `AddTransactionScreen.kt` | Active |
| `a.png` | **0.58 MB** | *None (Unreferenced)* | Unused |
| `f.png` | **0.47 MB** | *None (Unreferenced)* | Unused |
| `d.png` | **0.41 MB** | *None (Unreferenced)* | Unused |
| `c.png` | **0.39 MB** | *None (Unreferenced)* | Unused |
| `b.png` | **0.25 MB** | *None (Unreferenced)* | Unused |
| `h.png` | **0.22 MB** | *None (Unreferenced)* | Unused |

* **Active Onboarding & Account images (`ac.png` + `n1-n6.png`):** **~39.8 MB**
* **Unreferenced dead assets (`a.png` through `j.png`):** **~6.5 MB**

---

### D. Compiled DEX Bytecode (`classes*.dex` — 21.72 MB)
* The debug build generates 20 multidex files (`classes.dex` to `classes20.dex`).
* Total uncompressed bytecode size: **~72 MB**.
* Because `isMinifyEnabled = false`, ProGuard/R8 dead-code elimination is not performed, leaving all classes and methods from heavy dependencies (Jetpack Compose, Material3 Extended Icons, CameraX, Room, ML Kit, SQLCipher, Sherpa) in the binary.

---

## 3. Recommended Optimization Roadmap

By applying standard Android packaging best practices, the APK download size can be reduced from **~302 MB to under ~85 MB** without losing any features:

| Optimization | Action | Potential Savings | Projected Size |
| :--- | :--- | :--- | :--- |
| **1. ABI Filtering / App Bundles** | Split by architecture or target `arm64-v8a` for release devices | **~128 MB** | ~174 MB |
| **2. Convert Images to WebP** | Convert `ac.png` and `n1`–`n6` to lossy WebP format (80-85% quality) | **~35 MB** | ~139 MB |
| **3. Remove Dead Assets** | Delete unreferenced `a.png` through `j.png` | **~6.5 MB** | ~132.5 MB |
| **4. Enable R8 Minification** | Set `isMinifyEnabled = true` & `isShrinkResources = true` | **~15 MB** | **~85 MB** |

### Implementation Guide:

#### 1. ABI Filtering (in `app/build.gradle.kts`):
```kotlin
android {
    defaultConfig {
        ndk {
            // Include only physical phone architecture for testing/sideloading
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }
}
```
*(Or build an Android App Bundle (`.aab`) via `./gradlew bundleRelease` for Google Play Store delivery, which automatically serves single-ABI downloads to devices).*

#### 2. Resource Optimization:
* Use Android Studio's built-in **"Convert to WebP"** (Right-click `drawable` -> *Convert to WebP...* with 80% encoding quality).
* Delete unreferenced assets (`a.png` to `j.png`).

#### 3. Enable Shrinking (for Release builds):
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

package com.chirag.arthix.ocr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.chirag.arthix.R
import com.chirag.arthix.data.model.ConfidenceFlag
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.foundation.shape.RoundedCornerShape
/**
 * Single-screen Activity for FR-4 camera OCR logging.
 *
 * Responsibilities:
 * 1. Request CAMERA permission if not already granted.
 * 2. Start a CameraX preview.
 * 3. On "Capture" button tap: take a still image → feed to ML Kit TextRecognizer.
 * 4. Run [OcrAmountExtractor] + [OcrVendorExtractor] on the OCR text.
 * 5. Build an [OcrResultBundle] and route to [com.chirag.arthix.ui.screen.manual.ManualEntryScreen]
 *    via intent extras (all paths — confident or low-confidence — go through the
 *    same confirmation screen per EC-31).
 *
 * ## Latency target (EC-34)
 * The capture → prefill round-trip must complete in ≤ 4 seconds on the demo
 * device. ML Kit's on-device Latin model is typically < 1s after warm-up;
 * CameraX image capture adds ~0.5–1s. Test on actual device during Phase 7.
 *
 * ## Fallback (EC-31)
 * Faded thermal / handwritten receipts where OCR produces no reliable amount
 * still navigate to ManualEntryScreen with null [OcrResultBundle.amountPaise]
 * and [OcrResultBundle.isLowConfidence] = true — the user always lands on a
 * prefill screen, never a blank screen or an unexplained error.
 */
@AndroidEntryPoint
class ReceiptCaptureActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ReceiptCapture"

        /** Key for the extra written into the ManualEntryScreen Intent. */
        const val EXTRA_PREFILL_AMOUNT = "ocr_prefill_amount"
        const val EXTRA_PREFILL_PAYEE = "ocr_prefill_payee"
        const val EXTRA_PREFILL_CONFIDENCE = "ocr_prefill_confidence"
        const val EXTRA_IS_LOW_CONFIDENCE = "ocr_is_low_confidence"

        /** Helper to build the launch intent from anywhere in the app. */
        fun createIntent(context: Context): Intent =
            Intent(context, ReceiptCaptureActivity::class.java)
    }

    @Inject
    lateinit var textRecognizer: TextRecognizer

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startCamera()
        else {
            Toast.makeText(this, "Camera permission is required for receipt scanning", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_capture)

        previewView = findViewById(R.id.preview_view)
        cameraExecutor = Executors.newSingleThreadExecutor()

        checkCameraPermissionAndStart()

        val composeView = findViewById<androidx.compose.ui.platform.ComposeView>(R.id.compose_view)
        composeView.setContent {
            var isCapturing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

            androidx.compose.material3.MaterialTheme {
                androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    // Viewfinder frame
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.65f)
                            .padding(24.dp)
                            .align(androidx.compose.ui.Alignment.TopCenter)
                            .padding(top = 48.dp)
                            .border(2.dp, androidx.compose.ui.graphics.Color(0xFFE4463A), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    )

                    // Bottom control bar
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .align(androidx.compose.ui.Alignment.BottomCenter)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(androidx.compose.ui.graphics.Color(0xFFFAF7F2))
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.Button(
                            onClick = {
                                isCapturing = true
                                captureAndProcess()
                            },
                            enabled = !isCapturing,
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(64.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFFE4463A),
                                contentColor = androidx.compose.ui.graphics.Color.White
                            )
                        ) {
                            if (isCapturing) {
                                androidx.compose.material3.CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, modifier = androidx.compose.ui.Modifier.size(24.dp), strokeWidth = 2.dp)
                                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.width(12.dp))
                                androidx.compose.material3.Text("Processing...", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 18.sp)
                            } else {
                                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.CameraAlt, contentDescription = null)
                                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.width(12.dp))
                                androidx.compose.material3.Text("Scan Receipt", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }

                    // Visual feedback overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isCapturing,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(),
                        modifier = androidx.compose.ui.Modifier.fillMaxSize()
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    // ── Permission ─────────────────────────────────────────────────────────────

    private fun checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ── CameraX ────────────────────────────────────────────────────────────────

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                Toast.makeText(this, "Camera unavailable", Toast.LENGTH_SHORT).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ── Capture & OCR ──────────────────────────────────────────────────────────

    private fun captureAndProcess() {
        if (!::imageCapture.isInitialized) return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processImage(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed", exception)
                    // Route to blank manual entry — never crash or hang (EC-31).
                    routeToManualEntry(buildLowConfidenceBundle(rawText = ""))
                }
            }
        )
    }

    private fun processImage(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            routeToManualEntry(buildLowConfidenceBundle(rawText = ""))
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                image.close()
                val rawText = visionText.text
                Log.d(TAG, "OCR completed, text length=${rawText.length}")
                val bundle = buildBundle(rawText)
                routeToManualEntry(bundle)
            }
            .addOnFailureListener { e ->
                image.close()
                Log.e(TAG, "OCR processing failed", e)
                routeToManualEntry(buildLowConfidenceBundle(rawText = ""))
            }
    }

    // ── Bundle building ────────────────────────────────────────────────────────

    /**
     * Orchestrates [OcrAmountExtractor] + [OcrVendorExtractor] and maps the results
     * into an [OcrResultBundle].
     *
     * Amount resolution outcomes:
     * - [OcrAmountResult.Found] with keywordMatch=true → CLEAN, not low-confidence
     * - [OcrAmountResult.Found] with keywordMatch=false → NEEDS_REVIEW, low-confidence
     * - [OcrAmountResult.OutOfBounds] → NEEDS_REVIEW, low-confidence, amountPaise null
     * - [OcrAmountResult.NotFound] → NEEDS_REVIEW, low-confidence, amountPaise null
     */
    private fun buildBundle(rawText: String): OcrResultBundle {
        val amountResult = OcrAmountExtractor.extract(rawText)
        val vendor = OcrVendorExtractor.extract(rawText)

        return when (amountResult) {
            is OcrAmountResult.Found -> {
                val isKeyword = amountResult.isKeywordMatch
                OcrResultBundle(
                    amountPaise = amountResult.amountPaise,
                    payee = vendor,
                    confidenceFlag = if (isKeyword) ConfidenceFlag.CLEAN else ConfidenceFlag.NEEDS_REVIEW,
                    rawText = rawText,
                    isLowConfidence = !isKeyword,
                )
            }
            is OcrAmountResult.OutOfBounds -> {
                Log.w(TAG, "OCR amount out of bounds: ${amountResult.rawText}")
                OcrResultBundle(
                    amountPaise = null,
                    payee = vendor,
                    confidenceFlag = ConfidenceFlag.NEEDS_REVIEW,
                    rawText = rawText,
                    isLowConfidence = true,
                )
            }
            OcrAmountResult.NotFound -> buildLowConfidenceBundle(rawText = rawText, payee = vendor)
        }
    }

    private fun buildLowConfidenceBundle(rawText: String, payee: String? = null) = OcrResultBundle(
        amountPaise = null,
        payee = payee,
        confidenceFlag = ConfidenceFlag.NEEDS_REVIEW,
        rawText = rawText,
        isLowConfidence = true,
    )

    // ── Routing ────────────────────────────────────────────────────────────────

    /**
     * Navigates to the ManualEntryScreen (Phase 3) with OCR prefill data.
     *
     * All outcomes — high confidence AND low confidence — route here (EC-31).
     * The manual entry screen is the universal confirmation step for OCR results.
     * When [bundle.isLowConfidence] is true, the screen should surface a warning
     * (Phase 3's responsibility via [EXTRA_IS_LOW_CONFIDENCE]).
     */
    private fun routeToManualEntry(bundle: OcrResultBundle) {
        // Convert paise to display rupees string (AmountParser format: "450.00")
        val amountString = bundle.amountPaise?.let { paise ->
            val rupees = paise / 100
            val paiseRemainder = paise % 100
            "%d.%02d".format(rupees, paiseRemainder)
        }

        val resultIntent = Intent().apply {
            putExtra(EXTRA_PREFILL_AMOUNT, amountString)
            putExtra(EXTRA_PREFILL_PAYEE, bundle.payee)
            putExtra(EXTRA_PREFILL_CONFIDENCE, bundle.confidenceFlag.name)
            putExtra(EXTRA_IS_LOW_CONFIDENCE, bundle.isLowConfidence)
        }
        setResult(RESULT_OK, resultIntent)

        // Navigate to ManualEntryScreen via the app's main Activity / nav graph.
        // We broadcast an Intent that the main activity picks up and navigates with.
        val intent = Intent("com.chirag.arthix.action.OPEN_MANUAL_ENTRY").apply {
            setPackage(packageName)
            putExtra(EXTRA_PREFILL_AMOUNT, amountString)
            putExtra(EXTRA_PREFILL_PAYEE, bundle.payee)
            putExtra(EXTRA_PREFILL_CONFIDENCE, bundle.confidenceFlag.name)
            putExtra(EXTRA_IS_LOW_CONFIDENCE, bundle.isLowConfidence)
        }
        sendBroadcast(intent)

        Log.d(TAG, "Routing to manual entry: " +
            "amount=$amountString payee=${bundle.payee} " +
            "confidence=${bundle.confidenceFlag} lowConfidence=${bundle.isLowConfidence}")

        finish()
    }
}

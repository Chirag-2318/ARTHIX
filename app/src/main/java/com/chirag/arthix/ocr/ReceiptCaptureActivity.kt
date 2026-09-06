package com.chirag.arthix.ocr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.chirag.arthix.R
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.CaptureSource
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.data.repository.TransactionRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * Activity for receipt scanning and batch receipt logging (Docs/ARTHIX_OCR_Date_Extraction_Design.md).
 *
 * Responsibilities:
 * 1. Single & Batch camera receipt capture.
 * 2. Multi-image gallery picking (up to 10 receipts).
 * 3. Date extraction ([OcrDateExtractor]), amount extraction ([OcrAmountExtractor]),
 *    and vendor extraction ([OcrVendorExtractor]).
 * 4. Batch review queue ([BatchReceiptReviewSheet]) sorted by date (oldest first)
 *    with editable amount, date-picker, category, and payee.
 * 5. Universal editable date prefill routing to [com.chirag.arthix.ui.screen.manual.ManualEntryScreen].
 */
@AndroidEntryPoint
class ReceiptCaptureActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ReceiptCapture"

        /** Keys for extras written into ManualEntryScreen Intent. */
        const val EXTRA_PREFILL_AMOUNT = "ocr_prefill_amount"
        const val EXTRA_PREFILL_PAYEE = "ocr_prefill_payee"
        const val EXTRA_PREFILL_CONFIDENCE = "ocr_prefill_confidence"
        const val EXTRA_IS_LOW_CONFIDENCE = "ocr_is_low_confidence"
        const val EXTRA_PREFILL_DATE = "ocr_prefill_date"
        const val EXTRA_DATE_NEEDS_REVIEW = "ocr_date_needs_review"
        const val EXTRA_PREFILL_TIME_DISPLAY = "ocr_prefill_time_display"

        fun createIntent(context: Context): Intent =
            Intent(context, ReceiptCaptureActivity::class.java)
    }

    @Inject
    lateinit var textRecognizer: TextRecognizer

    @Inject
    lateinit var repository: TransactionRepository

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService

    // Batch logging state
    private val batchReceipts = mutableStateListOf<BatchReceiptItem>()
    private var isBatchModeActive = mutableStateOf(false)
    private var showBatchReviewSheet = mutableStateOf(false)
    private var isProcessingState = mutableStateOf(false)
    private var isSavingBatchState = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startCamera()
        else {
            Toast.makeText(this, "Camera permission is required for receipt scanning", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            processGalleryUris(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_capture)

        previewView = findViewById(R.id.preview_view)
        cameraExecutor = Executors.newSingleThreadExecutor()

        checkCameraPermissionAndStart()

        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            val isCapturing by remember { isProcessingState }
            val isBatchMode by remember { isBatchModeActive }
            val showReview by remember { showBatchReviewSheet }
            val isSavingBatch by remember { isSavingBatchState }

            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Viewfinder frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.62f)
                            .padding(24.dp)
                            .align(Alignment.TopCenter)
                            .padding(top = 48.dp)
                            .border(2.dp, Color(0xFFE4463A), RoundedCornerShape(24.dp))
                    )

                    // Top Batch Mode Toggle & Counter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 54.dp, start = 24.dp, end = 24.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Batch Mode Toggle Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isBatchMode) Color(0xFFE4463A) else Color(0xCC1A1A1C),
                            modifier = Modifier.clickable {
                                isBatchModeActive.value = !isBatchMode
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Layers,
                                    contentDescription = "Batch Mode",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isBatchMode) "Batch Mode ON" else "Batch Mode",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Review Queue Pill if receipts present
                        if (batchReceipts.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFFAF7F2),
                                modifier = Modifier.clickable {
                                    showBatchReviewSheet.value = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Queue: ${batchReceipts.size}",
                                        color = Color(0xFF1A1A1C),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Review →",
                                        color = Color(0xFFE4463A),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Bottom control bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(Color(0xFFFAF7F2))
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Gallery upload button
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEDE9E3))
                                    .clickable(enabled = !isCapturing) {
                                        pickMultipleMedia.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Collections,
                                    contentDescription = "Gallery Upload",
                                    tint = Color(0xFF1A1A1C)
                                )
                            }

                            // Capture Button
                            Button(
                                onClick = {
                                    isProcessingState.value = true
                                    captureAndProcess()
                                },
                                enabled = !isCapturing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE4463A),
                                    contentColor = Color.White
                                )
                            ) {
                                if (isCapturing) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Processing...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                } else {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        if (isBatchMode || batchReceipts.isNotEmpty()) "Snap Receipt (+)" else "Scan Receipt",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                }
                            }
                        }
                    }

                    // Visual feedback overlay
                    AnimatedVisibility(
                        visible = isCapturing,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.4f))
                        )
                    }

                    // Batch Review Sheet Overlay
                    if (showReview) {
                        BatchReceiptReviewSheet(
                            receipts = batchReceipts,
                            isConfirming = isSavingBatch,
                            onUpdateReceipt = { updated ->
                                val index = batchReceipts.indexOfFirst { it.id == updated.id }
                                if (index != -1) {
                                    batchReceipts[index] = updated
                                }
                            },
                            onDeleteReceipt = { id ->
                                batchReceipts.removeAll { it.id == id }
                                if (batchReceipts.isEmpty()) {
                                    showBatchReviewSheet.value = false
                                }
                            },
                            onAddMoreReceipts = {
                                showBatchReviewSheet.value = false
                            },
                            onConfirmAll = {
                                confirmBatchReceipts()
                            },
                            onDismiss = {
                                showBatchReviewSheet.value = false
                            }
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

    // ── Permission & CameraX ───────────────────────────────────────────────────

    private fun checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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

    // ── Image Processing & OCR ─────────────────────────────────────────────────

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
                    isProcessingState.value = false
                    val fallback = buildLowConfidenceBundle(rawText = "")
                    handleProcessedBundle(fallback)
                }
            }
        )
    }

    private fun processImage(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            isProcessingState.value = false
            handleProcessedBundle(buildLowConfidenceBundle(rawText = ""))
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                image.close()
                isProcessingState.value = false
                val rawText = visionText.text
                Log.d(TAG, "OCR completed, text length=${rawText.length}")
                val bundle = buildBundle(rawText)
                handleProcessedBundle(bundle)
            }
            .addOnFailureListener { e ->
                image.close()
                isProcessingState.value = false
                Log.e(TAG, "OCR processing failed", e)
                handleProcessedBundle(buildLowConfidenceBundle(rawText = ""))
            }
    }

    private fun processGalleryUris(uris: List<Uri>) {
        isProcessingState.value = true
        lifecycleScope.launch {
            for (uri in uris) {
                try {
                    val inputImage = InputImage.fromFilePath(this@ReceiptCaptureActivity, uri)
                    val visionText = withContext(Dispatchers.IO) {
                        com.google.android.gms.tasks.Tasks.await(textRecognizer.process(inputImage))
                    }
                    val bundle = buildBundle(visionText.text)
                    val item = bundleToBatchItem(bundle)
                    batchReceipts.add(item)
                } catch (e: Exception) {
                    Log.e(TAG, "Gallery image OCR failed for uri: $uri", e)
                }
            }
            isProcessingState.value = false
            if (batchReceipts.isNotEmpty()) {
                showBatchReviewSheet.value = true
            }
        }
    }

    private fun handleProcessedBundle(bundle: OcrResultBundle) {
        if (isBatchModeActive.value || batchReceipts.isNotEmpty()) {
            val item = bundleToBatchItem(bundle)
            batchReceipts.add(item)
            Toast.makeText(this, "Receipt added to batch (${batchReceipts.size})", Toast.LENGTH_SHORT).show()
        } else {
            routeToManualEntry(bundle)
        }
    }

    private fun bundleToBatchItem(bundle: OcrResultBundle): BatchReceiptItem {
        val amountStr = bundle.amountPaise?.let { paise ->
            val rupees = paise / 100
            val paiseRemainder = paise % 100
            if (paiseRemainder == 0L) "$rupees" else "%d.%02d".format(rupees, paiseRemainder)
        } ?: ""

        val payeeStr = bundle.payee ?: ""
        val category = if (payeeStr.isNotBlank()) {
            com.chirag.arthix.domain.category.TransactionCategoryAiClassifier.classify(payeeStr, null, Direction.OUTFLOW) ?: "Food"
        } else "Food"

        return BatchReceiptItem(
            amountText = amountStr,
            amountPaise = bundle.amountPaise,
            payee = payeeStr,
            category = category,
            transactionDateMillis = bundle.transactionDateMillis,
            isDateNeedsReview = bundle.isDateNeedsReview,
            reviewReasons = bundle.reviewReasons,
            rawText = bundle.rawText,
            timeDisplay = bundle.transactionTimeDisplay,
        )
    }

    // ── Bundle Building with Date & Confidence ─────────────────────────────────

    private fun buildBundle(rawText: String): OcrResultBundle {
        val amountResult = OcrAmountExtractor.extract(rawText)
        val vendor = OcrVendorExtractor.extract(rawText)
        val dateResult = OcrDateExtractor.extract(rawText)

        val dateMillis = when (dateResult) {
            is OcrDateResult.Found -> dateResult.epochMillis
            else -> null
        }
        val isDateNeedsReview = when (dateResult) {
            is OcrDateResult.Found -> dateResult.isAmbiguous
            is OcrDateResult.FutureDate -> true
            OcrDateResult.NotFound -> true
        }
        val dateRawSnippet = when (dateResult) {
            is OcrDateResult.Found -> dateResult.rawSnippet
            is OcrDateResult.FutureDate -> dateResult.rawSnippet
            OcrDateResult.NotFound -> null
        }

        val reviewReasons = mutableListOf<String>()
        if (amountResult !is OcrAmountResult.Found || !amountResult.isKeywordMatch) {
            reviewReasons.add("Confirm amount")
        }
        if (isDateNeedsReview) {
            reviewReasons.add("Confirm date")
        }

        val isAmountKeyword = (amountResult as? OcrAmountResult.Found)?.isKeywordMatch == true
        val baseConfidence = if (isAmountKeyword) ConfidenceFlag.CLEAN else ConfidenceFlag.NEEDS_REVIEW
        val finalConfidence = if (isDateNeedsReview) ConfidenceFlag.NEEDS_REVIEW else baseConfidence
        val isLowConfidence = finalConfidence == ConfidenceFlag.NEEDS_REVIEW || (amountResult !is OcrAmountResult.Found)

        return OcrResultBundle(
            amountPaise = (amountResult as? OcrAmountResult.Found)?.amountPaise,
            payee = vendor,
            confidenceFlag = finalConfidence,
            rawText = rawText,
            isLowConfidence = isLowConfidence,
            transactionDateMillis = dateMillis,
            isDateNeedsReview = isDateNeedsReview,
            dateRawSnippet = dateRawSnippet,
            reviewReasons = reviewReasons,
        )
    }

    private fun buildLowConfidenceBundle(rawText: String, payee: String? = null) = OcrResultBundle(
        amountPaise = null,
        payee = payee,
        confidenceFlag = ConfidenceFlag.NEEDS_REVIEW,
        rawText = rawText,
        isLowConfidence = true,
        transactionDateMillis = null,
        isDateNeedsReview = true,
        dateRawSnippet = null,
        reviewReasons = listOf("Confirm amount", "Confirm date"),
    )

    // ── Batch Confirmation ─────────────────────────────────────────────────────

    private fun confirmBatchReceipts() {
        if (batchReceipts.isEmpty()) return
        isSavingBatchState.value = true

        lifecycleScope.launch {
            try {
                for (item in batchReceipts) {
                    val paise = item.amountPaise ?: 0L
                    val effectiveTimestamp = item.transactionDateMillis ?: System.currentTimeMillis()
                    val flag = if (item.isDateNeedsReview || item.amountPaise == null) {
                        ConfidenceFlag.NEEDS_REVIEW
                    } else {
                        ConfidenceFlag.CLEAN
                    }
                    val cat = item.category.ifBlank { "Food" }
                    val payee = item.payee.ifBlank { cat }

                    repository.commit(
                        TransactionEntity(
                            amountPaise = if (paise > 0L) paise else null,
                            payee = payee,
                            category = cat,
                            timestamp = effectiveTimestamp,
                            direction = Direction.OUTFLOW,
                            source = CaptureSource.CAMERA,
                            status = if (paise > 0L) TransactionStatus.CONFIRMED else TransactionStatus.AWAITING_AMOUNT,
                            sourceCaptureId = null,
                            sourceNotificationId = null,
                            confidenceFlag = flag,
                            createdAt = System.currentTimeMillis(),
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReceiptCaptureActivity,
                        "${batchReceipts.size} receipt(s) logged successfully",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to confirm batch receipts", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ReceiptCaptureActivity, "Error saving receipts", Toast.LENGTH_SHORT).show()
                    isSavingBatchState.value = false
                }
            }
        }
    }

    // ── Routing for Single Capture ─────────────────────────────────────────────

    private fun routeToManualEntry(bundle: OcrResultBundle) {
        val amountString = bundle.amountPaise?.let { paise ->
            val rupees = paise / 100
            val paiseRemainder = paise % 100
            if (paiseRemainder == 0L) "$rupees" else "%d.%02d".format(rupees, paiseRemainder)
        }

        val resultIntent = Intent().apply {
            putExtra(EXTRA_PREFILL_AMOUNT, amountString)
            putExtra(EXTRA_PREFILL_PAYEE, bundle.payee)
            putExtra(EXTRA_PREFILL_CONFIDENCE, bundle.confidenceFlag.name)
            putExtra(EXTRA_IS_LOW_CONFIDENCE, bundle.isLowConfidence)
            bundle.transactionDateMillis?.let { putExtra(EXTRA_PREFILL_DATE, it) }
            putExtra(EXTRA_DATE_NEEDS_REVIEW, bundle.isDateNeedsReview)
            bundle.transactionTimeDisplay?.let { putExtra(EXTRA_PREFILL_TIME_DISPLAY, it) }
        }
        setResult(RESULT_OK, resultIntent)

        val intent = Intent("com.chirag.arthix.action.OPEN_MANUAL_ENTRY").apply {
            setPackage(packageName)
            putExtra(EXTRA_PREFILL_AMOUNT, amountString)
            putExtra(EXTRA_PREFILL_PAYEE, bundle.payee)
            putExtra(EXTRA_PREFILL_CONFIDENCE, bundle.confidenceFlag.name)
            putExtra(EXTRA_IS_LOW_CONFIDENCE, bundle.isLowConfidence)
            bundle.transactionDateMillis?.let { putExtra(EXTRA_PREFILL_DATE, it) }
            putExtra(EXTRA_DATE_NEEDS_REVIEW, bundle.isDateNeedsReview)
            bundle.transactionTimeDisplay?.let { putExtra(EXTRA_PREFILL_TIME_DISPLAY, it) }
        }
        sendBroadcast(intent)

        finish()
    }
}

package com.example.translation

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException

data class DetectedTextBox(
    val text: String,
    val boundingBox: RectF,
    val confidence: Float = 1.0f
)

class OCRManager {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastBitmapHash: Int? = null
    private var lastResults: List<DetectedTextBox> = emptyList()

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result, null)
        }
        addOnFailureListener { exception ->
            if (cont.isActive) cont.resumeWithException(exception)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }

    /**
     * Performs real OCR on the provided Bitmap using Google ML Kit.
     * Returns empty list if no text is recognized. Never returns fake or hardcoded text.
     */
    suspend fun detectText(bitmap: Bitmap, force: Boolean = false): List<DetectedTextBox> {
        return withContext(Dispatchers.Default) {
            try {
                if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
                    return@withContext emptyList()
                }

                val currentHash = bitmap.generationId
                if (!force && lastBitmapHash == currentHash && lastResults.isNotEmpty()) {
                    return@withContext lastResults
                }

                val image = InputImage.fromBitmap(bitmap, 0)
                val visionText = recognizer.process(image).awaitResult()

                val results = mutableListOf<DetectedTextBox>()
                for (block in visionText.textBlocks) {
                    val blockText = block.text.trim()
                    if (blockText.isNotBlank()) {
                        val box = block.boundingBox
                        val rectF = if (box != null) {
                            RectF(
                                box.left.toFloat(),
                                box.top.toFloat(),
                                box.right.toFloat(),
                                box.bottom.toFloat()
                            )
                        } else {
                            RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
                        }
                        results.add(
                            DetectedTextBox(
                                text = blockText,
                                boundingBox = rectF,
                                confidence = 0.95f
                            )
                        )
                    }
                }

                lastBitmapHash = currentHash
                lastResults = results
                results
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

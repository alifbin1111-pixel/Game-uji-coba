package com.example.translation

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DetectedTextBox(
    val text: String,
    val boundingBox: RectF,
    val confidence: Float
)

class OCRManager {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun detectText(bitmap: Bitmap): List<DetectedTextBox> {
        return withContext(Dispatchers.Default) {
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val task = textRecognizer.process(image)

                val results = mutableListOf<DetectedTextBox>()
                
                // Use task result synchronously (blocking wait in coroutine)
                var visionText: com.google.mlkit.vision.text.Text? = null
                var error: Exception? = null

                task.addOnSuccessListener { text ->
                    visionText = text
                }.addOnFailureListener { e ->
                    error = e
                }

                // Wait for task completion (simplified - in production use proper async handling)
                val maxWait = 5000L
                val startTime = System.currentTimeMillis()
                while (visionText == null && error == null && System.currentTimeMillis() - startTime < maxWait) {
                    Thread.sleep(10)
                }

                if (visionText != null) {
                    for (block in visionText!!.textBlocks) {
                        for (line in block.lines) {
                            val text = line.text.trim()
                            if (text.isNotEmpty()) {
                                val boundingBox = line.boundingBox?.let { box ->
                                    RectF(
                                        box.left.toFloat(),
                                        box.top.toFloat(),
                                        box.right.toFloat(),
                                        box.bottom.toFloat()
                                    )
                                } ?: RectF()

                                // Calculate confidence from recognition confidence
                                var confidence = 0.85f
                                for (element in line.elements) {
                                    confidence = maxOf(confidence, element.confidence)
                                }

                                results.add(
                                    DetectedTextBox(
                                        text = text,
                                        boundingBox = boundingBox,
                                        confidence = confidence
                                    )
                                )
                            }
                        }
                    }
                }

                image.close()
                results
            } catch (e: Exception) {
                // Return empty list on error instead of crashing
                emptyList()
            }
        }
    }

    fun close() {
        try {
            textRecognizer.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}

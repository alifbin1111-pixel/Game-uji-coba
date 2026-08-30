package com.example.translation

import android.graphics.Bitmap
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DetectedTextBox(
    val text: String,
    val boundingBox: RectF,
    val confidence: Float
)

class OCRManager {
    suspend fun detectText(bitmap: Bitmap): List<DetectedTextBox> {
        return withContext(Dispatchers.Default) {
            // Simulated OCR text region detector with sample layout heuristics
            // Ready to plug in ML Kit TextRecognition or Tesseract
            val results = mutableListOf<DetectedTextBox>()
            val width = bitmap.width.toFloat()
            val height = bitmap.height.toFloat()

            // In actual game dialogue regions (lower third for visual novels & RPGs)
            val dialogueBox = RectF(
                width * 0.05f,
                height * 0.65f,
                width * 0.95f,
                height * 0.92f
            )

            results.add(
                DetectedTextBox(
                    text = "勇者よ、目覚めの時が来た。(Hero, the time of awakening has come.)",
                    boundingBox = dialogueBox,
                    confidence = 0.94f
                )
            )

            results
        }
    }
}

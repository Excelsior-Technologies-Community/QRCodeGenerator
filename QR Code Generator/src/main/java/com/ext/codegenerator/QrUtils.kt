package com.ext.codegenerator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import java.util.EnumMap

object QRUtils {

    /**
     * Generate QR Code bitmap
     * @param text Text to encode in QR
     * @param width QR code width (default: 512)
     * @param height QR code height (default: 512)
     * @param overlayImage Optional image to overlay on QR code center
     * @param onSuccess Callback for successful generation
     * @param onError Callback for error
     */
    fun generateQR(
        text: String,
        width: Int = 512,
        height: Int = 512,
        overlayImage: Bitmap? = null,
        onSuccess: (Bitmap) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            if (text.isEmpty()) {
                onError("Input text cannot be empty")
                return
            }

            val qrBitmap = createQRCodeBitmap(text, width, height)

            val finalBitmap = if (overlayImage != null) {
                overlayImageOnQR(qrBitmap, overlayImage)
            } else {
                qrBitmap
            }

            onSuccess(finalBitmap)

        } catch (e: Exception) {
            onError("Error generating QR Code: ${e.message}")
        }
    }

    /**
     * Generate QR Code with text only
     */
    fun generateSimpleQR(
        text: String,
        size: Int = 512
    ): Bitmap? {
        return try {
            createQRCodeBitmap(text, size, size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generate QR Code with overlay image
     */
    fun generateQRWithOverlay(
        text: String,
        overlayImage: Bitmap,
        size: Int = 512
    ): Bitmap? {
        return try {
            val qrBitmap = createQRCodeBitmap(text, size, size)
            overlayImageOnQR(qrBitmap, overlayImage)
        } catch (e: Exception) {
            null
        }
    }

    private fun createQRCodeBitmap(text: String, width: Int, height: Int): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 2)
        }

        try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            )

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            return bitmap

        } catch (e: WriterException) {
            throw RuntimeException("Error generating QR Code", e)
        }
    }

    private fun overlayImageOnQR(qrBitmap: Bitmap, overlayBitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(qrBitmap.width, qrBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw the QR code
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)

        // Calculate the size and position for the overlay image (15% of QR code size)
        val overlaySize = (qrBitmap.width * 0.15f).toInt()
        val left = (qrBitmap.width - overlaySize) / 2
        val top = (qrBitmap.height - overlaySize) / 2

        // Create a circular overlay
        val scaledOverlay = Bitmap.createScaledBitmap(overlayBitmap, overlaySize, overlaySize, true)
        val circularOverlay = getCircularBitmap(scaledOverlay)

        // Draw white background circle for better visibility
        val backgroundPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        val radius = overlaySize / 2f + 4f // Reduced padding
        canvas.drawCircle(
            left + overlaySize / 2f,
            top + overlaySize / 2f,
            radius,
            backgroundPaint
        )

        // Draw the circular overlay image
        canvas.drawBitmap(circularOverlay, left.toFloat(), top.toFloat(), null)

        return result
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
        }

        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)

        canvas.drawOval(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }
}
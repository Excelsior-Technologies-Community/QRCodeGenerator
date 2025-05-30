package com.ext.qrcodelibrary.utils

import android.graphics.*
import android.graphics.drawable.GradientDrawable

object ImageUtils {
    fun createCircularBitmap(original: Bitmap, borderColor: Int = Color.WHITE, borderWidth: Float = 4f): Bitmap {
        val output = Bitmap.createBitmap(
            original.width,
            original.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val centerX = original.width / 2f
        val centerY = original.height / 2f
        val radius = minOf(centerX, centerY)

        // Draw border
        if (borderWidth > 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = borderWidth
            paint.color = borderColor
            canvas.drawCircle(centerX, centerY, radius - borderWidth / 2, paint)
        }

        // Create circular clip path
        val path = Path().apply {
            addCircle(centerX, centerY, radius - borderWidth, Path.Direction.CW)
        }

        // Draw the circular image
        canvas.clipPath(path)
        canvas.drawBitmap(original, 0f, 0f, paint)

        return output
    }

    fun createQRBackground(size: Int, backgroundColor: Int = Color.WHITE): Bitmap {
        val background = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(background)
        canvas.drawColor(backgroundColor)
        return background
    }

    fun addGlowEffect(bitmap: Bitmap, glowColor: Int = Color.WHITE, glowRadius: Float = 10f): Bitmap {
        val output = Bitmap.createBitmap(
            bitmap.width + (2 * glowRadius).toInt(),
            bitmap.height + (2 * glowRadius).toInt(),
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.OUTER)
        paint.color = glowColor

        canvas.drawBitmap(
            bitmap,
            glowRadius,
            glowRadius,
            paint
        )

        canvas.drawBitmap(
            bitmap,
            glowRadius,
            glowRadius,
            Paint()
        )

        return output
    }
} 
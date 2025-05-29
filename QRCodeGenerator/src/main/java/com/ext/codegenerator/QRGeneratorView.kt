package com.ext.codegenerator

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import java.io.IOException
import java.io.OutputStream
import java.util.EnumMap

class QRGeneratorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    // UI Components
    private lateinit var etInputText: TextInputEditText
    private lateinit var btnSelectImage: Button
    private lateinit var btnGenerate: Button
    private lateinit var btnDownload: Button
    private lateinit var ivSelectedImage: ImageView
    private lateinit var ivQrCode: ImageView
    private lateinit var cvQrCode: CardView

    // Data
    private var selectedImageBitmap: Bitmap? = null
    private var generatedQrBitmap: Bitmap? = null

    // Activity-related components
    private var imagePickerLauncher: ActivityResultLauncher<Intent>? = null
    private val STORAGE_PERMISSION_CODE = 1000

    // Callback interfaces
    interface QRGeneratorCallback {
        fun onQRGenerated(bitmap: Bitmap)
        fun onQRSaved()
        fun onError(message: String)
    }

    private var callback: QRGeneratorCallback? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_qr_generator, this, true)
        initViews()
        setupClickListeners()
        setupImagePicker()
    }

    private fun initViews() {
        etInputText = findViewById(R.id.etInputText)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnDownload = findViewById(R.id.btnDownload)
        ivSelectedImage = findViewById(R.id.ivSelectedImage)
        ivQrCode = findViewById(R.id.ivQrCode)
        cvQrCode = findViewById(R.id.cvQrCode)
    }

    private fun setupClickListeners() {
        btnSelectImage.setOnClickListener {
            openImagePicker()
        }

        btnGenerate.setOnClickListener {
            generateQrCode()
        }

        btnDownload.setOnClickListener {
            downloadQrCode()
        }
    }

    private fun setupImagePicker() {
        if (context is AppCompatActivity) {
            imagePickerLauncher = (context as AppCompatActivity).registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        try {
                            selectedImageBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            ivSelectedImage.setImageBitmap(selectedImageBitmap)
                            showSelectedImage()
                        } catch (e: IOException) {
                            showError("Error loading image")
                        }
                    }
                }
            }
        }
    }

    // Public methods for external control
    fun setCallback(callback: QRGeneratorCallback) {
        this.callback = callback
    }

    fun setInputText(text: String) {
        etInputText.setText(text)
    }

    fun getInputText(): String = etInputText.text.toString().trim()

    fun setSelectedImage(bitmap: Bitmap) {
        selectedImageBitmap = bitmap
        ivSelectedImage.setImageBitmap(bitmap)
        showSelectedImage()
    }

    fun generateQRFromText(text: String) {
        etInputText.setText(text)
        generateQrCode()
    }

    // Private helper methods
    private fun showSelectedImage() {
        ivSelectedImage.visibility = VISIBLE
    }

    private fun hideSelectedImage() {
        ivSelectedImage.visibility = GONE
    }

    private fun showQrCode() {
        cvQrCode.visibility = VISIBLE
        btnDownload.visibility = VISIBLE
    }

    private fun hideQrCode() {
        cvQrCode.visibility = GONE
        btnDownload.visibility = GONE
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        callback?.onError(message)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher?.launch(intent)
    }

    private fun generateQrCode() {
        val inputText = getInputText()

        if (inputText.isEmpty()) {
            showError("Please enter text, URL, or emoji")
            return
        }

        try {
            val qrBitmap = createQRCodeBitmap(inputText, 512, 512)

            // If user selected an image, overlay it on the QR code
            generatedQrBitmap = if (selectedImageBitmap != null) {
                overlayImageOnQR(qrBitmap, selectedImageBitmap!!)
            } else {
                qrBitmap
            }

            ivQrCode.setImageBitmap(generatedQrBitmap)
            showQrCode()

            Toast.makeText(context, "QR Code generated successfully!", Toast.LENGTH_SHORT).show()
            generatedQrBitmap?.let { callback?.onQRGenerated(it) }

        } catch (e: Exception) {
            showError("Error generating QR Code: ${e.message}")
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

        // Calculate the size and position for the overlay image (25% of QR code size)
        val overlaySize = (qrBitmap.width * 0.25f).toInt()
        val left = (qrBitmap.width - overlaySize) / 2
        val top = (qrBitmap.height - overlaySize) / 2

        // Create a circular overlay with proper center cropping
        val scaledOverlay = createCenterCroppedBitmap(overlayBitmap, overlaySize)
        val circularOverlay = getCircularBitmap(scaledOverlay)

        // Draw white background circle with border for better visibility
        val backgroundPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val radius = overlaySize / 2f + 12f
        val centerX = left + overlaySize / 2f
        val centerY = top + overlaySize / 2f

        // Draw white background circle
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        // Draw black border
        canvas.drawCircle(centerX, centerY, radius, borderPaint)

        // Draw the circular overlay image
        canvas.drawBitmap(circularOverlay, left.toFloat(), top.toFloat(), null)

        return result
    }

    private fun createCenterCroppedBitmap(source: Bitmap, size: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height

        val xScale = size.toFloat() / sourceWidth
        val yScale = size.toFloat() / sourceHeight
        val scale = maxOf(xScale, yScale)

        val scaledWidth = (size / scale).toInt()
        val scaledHeight = (size / scale).toInt()
        val left = (sourceWidth - scaledWidth) / 2
        val top = (sourceHeight - scaledHeight) / 2

        val sourceRect = Rect(left, top, left + scaledWidth, top + scaledHeight)
        val destRect = Rect(0, 0, size, size)

        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, sourceRect, destRect, Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        })

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

        paint.apply {
            xfermode = null
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = 3f
        }
        canvas.drawOval(rectF, paint)

        return output
    }

    private fun downloadQrCode() {
        if (generatedQrBitmap == null) {
            showError("No QR code to download")
            return
        }

        if (context !is AppCompatActivity) {
            showError("Download requires Activity context")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToGallery()
        } else {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                saveImageToGallery()
            } else {
                ActivityCompat.requestPermissions(
                    context as AppCompatActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    private fun saveImageToGallery() {
        val filename = "QRCode_${System.currentTimeMillis()}.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                val outputStream: OutputStream? = context.contentResolver.openOutputStream(it)
                outputStream?.use { stream ->
                    generatedQrBitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                Toast.makeText(context, "QR Code saved to gallery!", Toast.LENGTH_SHORT).show()
                callback?.onQRSaved()

            } catch (e: IOException) {
                showError("Error saving QR Code: ${e.message}")
            }
        } ?: run {
            showError("Error creating file")
        }
    }

    // Handle permission results (should be called from Activity)
    fun handlePermissionResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImageToGallery()
            } else {
                showError("Storage permission denied")
            }
        }
    }
}
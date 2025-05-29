package com.ext.codegenerator

import android.Manifest
import android.app.Activity
import android.content.ContentValues
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
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import java.io.IOException
import java.io.OutputStream
import java.util.EnumMap

class QRGenerativeActivity : AppCompatActivity() {

    private lateinit var qrGeneratorView: QRGeneratorView

    private var selectedImageBitmap: Bitmap? = null
    private var generatedQrBitmap: Bitmap? = null

    private val STORAGE_PERMISSION_CODE = 1000

    // New constants for passing data
    companion object {
        const val EXTRA_INPUT_TEXT = "input_text"
        const val EXTRA_IMAGE_URI = "image_uri"
        const val EXTRA_AUTO_GENERATE = "auto_generate"

        // Result codes
        const val RESULT_QR_GENERATED = 1001
        const val RESULT_QR_SAVED = 1002
        const val EXTRA_QR_BITMAP = "qr_bitmap"
    }

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    qrGeneratorView.ivSelectedImage.setImageBitmap(selectedImageBitmap)
                    qrGeneratorView.showSelectedImage()
                } catch (e: IOException) {
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrgenerative)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupClickListeners()
        handleIncomingData()
    }

    private fun initViews() {
        qrGeneratorView = findViewById(R.id.qrGeneratorView)
    }

    private fun setupClickListeners() {
        qrGeneratorView.btnSelectImage.setOnClickListener {
            openImagePicker()
        }

        qrGeneratorView.btnGenerate.setOnClickListener {
            generateQrCode()
        }

        qrGeneratorView.btnDownload.setOnClickListener {
            downloadQrCode()
        }
    }

    private fun handleIncomingData() {
        // Handle pre-filled text
        intent.getStringExtra(EXTRA_INPUT_TEXT)?.let { inputText ->
            qrGeneratorView.setInputText(inputText)
        }

        // Handle pre-selected image
        intent.getStringExtra(EXTRA_IMAGE_URI)?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                qrGeneratorView.ivSelectedImage.setImageBitmap(selectedImageBitmap)
                qrGeneratorView.showSelectedImage()
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading provided image", Toast.LENGTH_SHORT).show()
            }
        }

        // Auto-generate if requested
        if (intent.getBooleanExtra(EXTRA_AUTO_GENERATE, false)) {
            generateQrCode()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun generateQrCode() {
        val inputText = qrGeneratorView.getInputText()

        if (inputText.isEmpty()) {
            Toast.makeText(this, "Please enter text, URL, or emoji", Toast.LENGTH_SHORT).show()
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

            qrGeneratorView.ivQrCode.setImageBitmap(generatedQrBitmap)
            qrGeneratorView.showQrCode()

            Toast.makeText(this, "QR Code generated successfully!", Toast.LENGTH_SHORT).show()

            // Send result back to calling activity
            val resultIntent = Intent().apply {
                putExtra(EXTRA_QR_BITMAP, generatedQrBitmap)
            }
            setResult(RESULT_QR_GENERATED, resultIntent)

        } catch (e: Exception) {
            Toast.makeText(this, "Error generating QR Code: ${e.message}", Toast.LENGTH_SHORT).show()
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

        // Compute the scaling factors
        val xScale = size.toFloat() / sourceWidth
        val yScale = size.toFloat() / sourceHeight
        val scale = maxOf(xScale, yScale)

        // Compute the source rectangle for center cropping
        val scaledWidth = (size / scale).toInt()
        val scaledHeight = (size / scale).toInt()
        val left = (sourceWidth - scaledWidth) / 2
        val top = (sourceHeight - scaledHeight) / 2

        // Create the source rectangle
        val sourceRect = Rect(left, top, left + scaledWidth, top + scaledHeight)
        val destRect = Rect(0, 0, size, size)

        // Create and draw the scaled bitmap
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

        // Draw white background
        canvas.drawOval(rectF, paint)

        // Draw the bitmap
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        // Draw border
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
            Toast.makeText(this, "No QR code to download", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 and above - use MediaStore
            saveImageToGallery()
        } else {
            // Below Android 10 - check storage permission
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                saveImageToGallery()
            } else {
                ActivityCompat.requestPermissions(
                    this,
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

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                val outputStream: OutputStream? = contentResolver.openOutputStream(it)
                outputStream?.use { stream ->
                    generatedQrBitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                Toast.makeText(this, "QR Code saved to gallery!", Toast.LENGTH_SHORT).show()

                // Send result back to calling activity
                setResult(RESULT_QR_SAVED)

            } catch (e: IOException) {
                Toast.makeText(this, "Error saving QR Code: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImageToGallery()
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
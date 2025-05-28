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
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ext.qrcodegenerator.R
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import java.io.IOException
import java.io.OutputStream
import java.util.EnumMap

class QRGenerativeActivity : AppCompatActivity() {

    private lateinit var etInputText: TextInputEditText
    private lateinit var btnSelectImage: Button
    private lateinit var btnGenerate: Button
    private lateinit var btnDownload: Button
    private lateinit var ivSelectedImage: ImageView
    private lateinit var ivQrCode: ImageView
    private lateinit var cvQrCode: CardView

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
                    ivSelectedImage.setImageBitmap(selectedImageBitmap)
                    ivSelectedImage.visibility = ImageView.VISIBLE
                } catch (e: IOException) {
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    private fun handleIncomingData() {
        // Handle pre-filled text
        intent.getStringExtra(EXTRA_INPUT_TEXT)?.let { inputText ->
            etInputText.setText(inputText)
        }

        // Handle pre-selected image
        intent.getStringExtra(EXTRA_IMAGE_URI)?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                ivSelectedImage.setImageBitmap(selectedImageBitmap)
                ivSelectedImage.visibility = ImageView.VISIBLE
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
        val inputText = etInputText.text.toString().trim()

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

            ivQrCode.setImageBitmap(generatedQrBitmap)
            cvQrCode.visibility = CardView.VISIBLE
            btnDownload.visibility = Button.VISIBLE

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
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.MARGIN] = 1

        try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            )

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

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
        val result = Bitmap.createBitmap(qrBitmap.width, qrBitmap.height, qrBitmap.config)
        val canvas = Canvas(result)

        // Draw the QR code
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)

        // Calculate the size and position for the overlay image (20% of QR code size)
        val overlaySize = (qrBitmap.width * 0.2f).toInt()
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
        val radius = overlaySize / 2f + 8f
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
        }

        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)

        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

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
package com.ext.codegenerator

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Reader
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import java.io.IOException
import java.util.EnumMap

class QRScannerActivity : AppCompatActivity() {

    private lateinit var btnUploadImage: Button
    private lateinit var btnCaptureImage: Button
    private lateinit var btnRealtimeScan: Button
    private lateinit var ivScannedImage: ImageView
    private lateinit var tvResult: TextView
    private lateinit var cvResult: CardView

    private var capturedImageUri: Uri? = null
    private val CAMERA_PERMISSION_CODE = 1001

    companion object {
        const val EXTRA_SCANNED_TEXT = "scanned_text"
        const val RESULT_QR_SCANNED = 2001
    }

    // Real-time scanner launcher
    private val realtimeScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == CameraPreviewActivity.RESULT_QR_SCANNED) {
            result.data?.getStringExtra(CameraPreviewActivity.EXTRA_SCANNED_TEXT)?.let { text ->
                tvResult.text = text
                cvResult.visibility = CardView.VISIBLE
                Toast.makeText(this, "QR Code scanned successfully!", Toast.LENGTH_SHORT).show()

                // Send result back to calling activity
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_SCANNED_TEXT, text)
                }
                setResult(RESULT_QR_SCANNED, resultIntent)
            }
        }
    }

    // Image picker launcher for uploading from gallery
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    processImage(bitmap)
                } catch (e: IOException) {
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Camera launcher for capturing image
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            capturedImageUri?.let { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    processImage(bitmap)
                } catch (e: IOException) {
                    Toast.makeText(this, "Error processing captured image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_qrscanner)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnUploadImage = findViewById(R.id.btnUploadImage)
        btnCaptureImage = findViewById(R.id.btnCaptureImage)
        btnRealtimeScan = findViewById(R.id.btnRealtimeScan)
        ivScannedImage = findViewById(R.id.ivScannedImage)
        tvResult = findViewById(R.id.tvResult)
        cvResult = findViewById(R.id.cvResult)
    }

    private fun setupClickListeners() {
        btnUploadImage.setOnClickListener {
            openImagePicker()
        }

        btnCaptureImage.setOnClickListener {
            checkCameraPermissionAndCapture()
        }

        btnRealtimeScan.setOnClickListener {
            startRealtimeScanner()
        }
    }

    private fun startRealtimeScanner() {
        val intent = Intent(this, CameraPreviewActivity::class.java)
        realtimeScannerLauncher.launch(intent)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun checkCameraPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            captureImage()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun captureImage() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "QR_Scan_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.DESCRIPTION, "QR Scanner Capture")
        }

        capturedImageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri)
        }

        cameraLauncher.launch(intent)
    }

    private fun processImage(bitmap: Bitmap) {
        try {
            // Display the image
            ivScannedImage.setImageBitmap(bitmap)
            ivScannedImage.visibility = ImageView.VISIBLE

            // Try different image processing approaches
            val decodedText = decodeQRFromBitmap(bitmap) ?: decodeQRFromBitmapWithPreprocessing(bitmap)

            if (decodedText != null) {
                // Show result
                tvResult.text = decodedText
                cvResult.visibility = CardView.VISIBLE

                Toast.makeText(this, "QR Code scanned successfully!", Toast.LENGTH_SHORT).show()

                // Send result back to calling activity
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_SCANNED_TEXT, decodedText)
                }
                setResult(RESULT_QR_SCANNED, resultIntent)
            } else {
                tvResult.text = "No QR code found in the image"
                cvResult.visibility = CardView.VISIBLE
                Toast.makeText(this, "No QR code detected", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun decodeQRFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.TRY_HARDER, true)
                put(DecodeHintType.POSSIBLE_FORMATS, listOf(com.google.zxing.BarcodeFormat.QR_CODE))
            }

            val reader: Reader = MultiFormatReader()
            val result: Result = reader.decode(binaryBitmap, hints)

            result.text
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeQRFromBitmapWithPreprocessing(bitmap: Bitmap): String? {
        return try {
            // Scale the bitmap to improve processing
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
            
            // Convert to grayscale for better recognition
            val width = scaledBitmap.width
            val height = scaledBitmap.height
            val pixels = IntArray(width * height)
            scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            // Convert to grayscale
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                pixels[i] = (0xff shl 24) or (gray shl 16) or (gray shl 8) or gray
            }

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.TRY_HARDER, true)
                put(DecodeHintType.POSSIBLE_FORMATS, listOf(com.google.zxing.BarcodeFormat.QR_CODE))
                put(DecodeHintType.CHARACTER_SET, "UTF-8")
            }

            val reader: Reader = MultiFormatReader()
            val result: Result = reader.decode(binaryBitmap, hints)

            result.text
        } catch (e: Exception) {
            null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImage()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
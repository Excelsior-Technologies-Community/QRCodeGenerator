package com.ext.qrcodegenerator

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
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
import com.ext.codegenerator.QRCodeConfig
import com.ext.codegenerator.QRCodeView
import java.io.IOException
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var qrCodeView: QRCodeView
    private val STORAGE_PERMISSION_CODE = 1000

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                qrCodeView.handleImagePickerResult(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize QR Code View
        qrCodeView = findViewById(R.id.qrCodeView)

        // Example 1: Basic setup with default configuration
        setupBasicQRCode()

        // Example 2: Custom configuration
//         setupCustomQRCode()

        // Example 3: Minimal QR code
//         setupMinimalQRCode()

        // Example 4: Dark theme QR code
//         setupDarkThemeQRCode()

        // Example 5: Branded QR code
//         setupBrandedQRCode()
    }

    private fun setupBasicQRCode() {
        // Handle image picker request
        qrCodeView.setOnImagePickerRequestListener {
            openImagePicker()
        }

        qrCodeView.setOnQRGeneratedListener { bitmap ->
            Toast.makeText(this, "QR Code generated successfully!", Toast.LENGTH_SHORT).show()
        }

        qrCodeView.setOnImageSelectedListener { bitmap ->
            Toast.makeText(this, "Image selected for QR code center", Toast.LENGTH_SHORT).show()
        }

        qrCodeView.setOnDownloadClickListener { bitmap ->
            downloadQRCode(bitmap)
        }
    }

    private fun setupCustomQRCode() {
        // Create custom configuration using Builder pattern
        val customConfig = QRCodeConfig.Builder()
            .setTitle("My Custom QR Generator", 80f, Color.BLUE, true)
            .setInputHint("Enter your custom text here", 6)
            .setButtonTexts("Pick Image", "Create QR", "Save QR")
            .setQRCodeAppearance(800, Color.LTGRAY, Color.DKGRAY)
            .setOverlayConfiguration(0.3f, Color.RED, 6f, Color.YELLOW)
            .setVisibility(showTitle = true, showImageSelection = true, showDownloadButton = true)
            .build()

        qrCodeView.setConfiguration(customConfig)
        setupBasicQRCode() // Apply listeners
    }

    private fun setupMinimalQRCode() {
        val minimalConfig = QRCodeConfig.minimal()
        qrCodeView.setConfiguration(minimalConfig)
        setupBasicQRCode()
    }

    private fun setupDarkThemeQRCode() {
        val darkConfig = QRCodeConfig.darkTheme()
        qrCodeView.setConfiguration(darkConfig)
        setupBasicQRCode()
    }

    private fun setupBrandedQRCode() {
        val brandColor = Color.parseColor("#FF6B35") // Orange brand color
        val brandedConfig = QRCodeConfig.branded(brandColor, Color.WHITE, brandColor)
        qrCodeView.setConfiguration(brandedConfig)
        setupBasicQRCode()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun downloadQRCode(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToGallery(bitmap)
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                saveImageToGallery(bitmap)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
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
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                Toast.makeText(this, "QR Code saved to gallery!", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this, "Error saving QR Code: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
                qrCodeView.getGeneratedQRCode()?.let { bitmap ->
                    saveImageToGallery(bitmap)
                }
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Programmatic usage examples
    private fun programmaticExamples() {
        // Set text programmatically
        qrCodeView.setText("https://example.com")

        // Generate QR code with specific text
        qrCodeView.generateQRCodeWithText("Hello World!")

        // Get generated QR code bitmap
        val qrBitmap = qrCodeView.getGeneratedQRCode()

        // Set image from bitmap
        // qrCodeView.setImageFromBitmap(yourBitmap)

        // Get current configuration
        val currentConfig = qrCodeView.getConfiguration()

        // Modify and apply new configuration
        val modifiedConfig = currentConfig.copy(
            titleText = "Modified Title",
            qrForegroundColor = Color.RED
        )
        qrCodeView.setConfiguration(modifiedConfig)
    }
}
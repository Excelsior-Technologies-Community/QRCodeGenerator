package com.ext.qrcodegenerator

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.core.content.ContextCompat
import com.ext.qrcodegenerator.databinding.ActivityMainBinding
import com.ext.qrcodelibrary.QRCodeManager
import com.ext.qrcodelibrary.QRCodeView
import com.ext.qrcodelibrary.QRScannerView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var qrCodeManager: QRCodeManager
    private var currentScannedText: String? = null
    private var isScanMode = false
    private var selectedCenterImage: Bitmap? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScanning()
        } else {
            Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
        }
    }

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processSelectedImage(it) }
    }

    private val scanImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { scanQRFromImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        qrCodeManager = QRCodeManager(this)
        setupModeSwitch()
        setupGenerationControls()
        setupQRScannerView()
        setupResultButtons()
    }

    private fun setupModeSwitch() {
        binding.modeSwitch.setOnCheckedChangeListener { _, isChecked ->
            isScanMode = isChecked
            updateMode()
        }
    }

    private fun updateMode() {
        if (isScanMode) {
            binding.inputLayout.visibility = View.GONE
            binding.btnSelectImage.visibility = View.GONE
            binding.selectedImage.visibility = View.GONE
            binding.btnGenerate.visibility = View.GONE
            binding.qrCodeView.visibility = View.GONE
            binding.btnDownload.visibility = View.GONE
            binding.qrScannerView.visibility = View.VISIBLE
            binding.modeSwitch.text = "Switch to Generate Mode"
            checkCameraPermissionAndStartScanning()
        } else {
            binding.inputLayout.visibility = View.VISIBLE
            binding.btnSelectImage.visibility = View.VISIBLE
            binding.btnGenerate.visibility = View.VISIBLE
            binding.qrCodeView.visibility = View.VISIBLE
            binding.qrScannerView.visibility = View.GONE
            binding.modeSwitch.text = "Switch to Scan Mode"
            stopScanning()
        }
    }

    private fun setupGenerationControls() {
        binding.btnSelectImage.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        binding.btnGenerate.setOnClickListener {
            val text = binding.inputText.text.toString()
            if (text.isNotEmpty()) {
                binding.qrCodeView.setText(text)
                selectedCenterImage?.let { image ->
                    binding.qrCodeView.setImage(image)
                }
                binding.qrCodeView.generateQRCode()
                binding.btnDownload.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Please enter text for QR code", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDownload.setOnClickListener {
            binding.qrCodeView.getQRCodeBitmap()?.let { bitmap ->
                saveQRCodeToGallery(bitmap)

            }
        }
    }


    @OptIn(ExperimentalGetImage::class)
    private fun setupQRScannerView() {
        binding.qrScannerView.apply {
            startScanning(this@MainActivity) { result ->
                handleScanResult(result)
            }
        }
    }

    private fun setupResultButtons() {
        binding.btnCopy.setOnClickListener {
            currentScannedText?.let { text ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("QR Code Result", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnShare.setOnClickListener {
            currentScannedText?.let { text ->
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, "Share QR Code Result"))
            }
        }
    }

    private fun processSelectedImage(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                selectedCenterImage = bitmap
                binding.selectedImage.setImageBitmap(bitmap)
                binding.selectedImage.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveQRCodeToGallery(qrBitmap: Bitmap) {
        val fileName = "QR_${System.currentTimeMillis()}.png"
        val uri = qrCodeManager.saveQRCodeToGallery(this, qrBitmap, fileName)

        if (uri != null) {
            Toast.makeText(this, "QR code saved to gallery", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCameraPermissionAndStartScanning() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startScanning()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startScanning() {
        binding.qrScannerView.visibility = View.VISIBLE
        binding.qrScannerView.startScanning(this) { result ->
            handleScanResult(result)
        }
    }

    private fun stopScanning() {
        binding.qrScannerView.stopScanning()
    }

    private fun handleScanResult(result: String) {
        currentScannedText = result
        binding.resultText.text = result
        binding.btnCopy.visibility = View.VISIBLE
        binding.btnShare.visibility = View.VISIBLE
        
        // Generate QR code from scanned result and display it
        binding.qrCodeView.setText(result)
        binding.qrCodeView.generateQRCode()
        binding.btnDownload.visibility = View.VISIBLE
    }

    private fun scanQRFromImage(uri: Uri) {
        qrCodeManager.scanQRFromImage(uri)?.let { result ->
            handleScanResult(result)
        } ?: run {
            Toast.makeText(this, "No QR code found in the image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
    }
}
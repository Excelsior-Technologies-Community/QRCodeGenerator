package com.ext.qrcodegenerator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ext.qrcodelibrary.QRCodeManager
import com.ext.qrcodegenerator.databinding.ActivityMainBinding
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var qrCodeManager: QRCodeManager
    private var selectedCenterImage: Bitmap? = null
    private var generatedQRBitmap: Bitmap? = null

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
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSelectImage.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        binding.btnGenerate.setOnClickListener {
            generateQRCode()
        }

        binding.btnScan.setOnClickListener {
            checkCameraPermissionAndStartScanning()
        }

        binding.btnScanFromImage.setOnClickListener {
            scanImageLauncher.launch("image/*")
        }

        binding.btnDownload.setOnClickListener {
            saveQRCodeToGallery()
        }
    }

    private fun processSelectedImage(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                selectedCenterImage = BitmapFactory.decodeStream(inputStream)
                binding.selectedImage.setImageBitmap(selectedCenterImage)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateQRCode() {
        val content = binding.inputText.text?.toString()
        if (content.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter text to generate QR code", Toast.LENGTH_SHORT).show()
            return
        }

        val config = QRCodeManager.QRConfig(
            content = content,
            size = 512,
            centerImage = selectedCenterImage,
            centerImageSize = 0.2f,
            qrColor = Color.BLACK,
            backgroundColor = Color.WHITE,
            errorCorrectionLevel = ErrorCorrectionLevel.H,
            circleBorderColor = Color.WHITE,
            circleBorderWidth = 4f,
            addGlow = false
        )

        generatedQRBitmap = qrCodeManager.generateQRCode(config)

        binding.previewView.visibility = View.GONE
        binding.qrCodeImage.visibility = View.VISIBLE
        binding.qrCodeImage.setImageBitmap(generatedQRBitmap)
        binding.btnDownload.visibility = View.VISIBLE
    }

    private fun saveQRCodeToGallery() {
        generatedQRBitmap?.let { qrBitmap ->
            val fileName = "QR_${System.currentTimeMillis()}.png"
            val uri = qrCodeManager.saveQRCodeToGallery(this, qrBitmap, fileName)
            
            if (uri != null) {
                Toast.makeText(this, "QR code saved to gallery", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save QR code", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No QR code to save", Toast.LENGTH_SHORT).show()
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

    private fun startScanning() {
        binding.qrCodeImage.visibility = View.GONE
        binding.previewView.visibility = View.VISIBLE
        binding.btnDownload.visibility = View.GONE

        qrCodeManager.startQRCodeScanning(
            previewView = binding.previewView,
            lifecycleOwner = this,
            scanAreaSize = 0.8f
        ) { result ->
            runOnUiThread {
                binding.resultText.text = result
                binding.previewView.visibility = View.GONE
            }
        }
    }

    private fun scanQRFromImage(uri: Uri) {
        qrCodeManager.scanQRFromImage(uri)?.let { result ->
            binding.resultText.text = result
        } ?: run {
            Toast.makeText(this, "No QR code found in the image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        qrCodeManager.stopScanning()
    }
}
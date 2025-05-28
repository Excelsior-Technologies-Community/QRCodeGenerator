package com.ext.qrcodegenerator

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ext.codegenerator.QRGenerativeActivity
import com.ext.codegenerator.QRScannerActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnGenerateQR: Button
    private lateinit var btnScanQR: Button
    private lateinit var ivResult: ImageView
    private lateinit var tvScannedText: TextView

    // Launcher for QR Scanner Activity
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == QRScannerActivity.RESULT_QR_SCANNED) {
            val scannedText = result.data?.getStringExtra(QRScannerActivity.EXTRA_SCANNED_TEXT)
            scannedText?.let {
                tvScannedText.text = "Scanned: $it"
                tvScannedText.visibility = TextView.VISIBLE
                Toast.makeText(this, "QR Code scanned successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private val qrGeneratorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            QRGenerativeActivity.RESULT_QR_GENERATED -> {
                Toast.makeText(this, "QR Code generated successfully!", Toast.LENGTH_SHORT).show()
                // You can get the bitmap if needed
                val bitmap = result.data?.getParcelableExtra<Bitmap>(QRGenerativeActivity.EXTRA_QR_BITMAP)
                bitmap?.let {
                    ivResult.setImageBitmap(it)
                }
            }
            QRGenerativeActivity.RESULT_QR_SAVED -> {
                Toast.makeText(this, "QR Code saved to gallery!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnGenerateQR = findViewById(R.id.btnGenerateQR)
        btnScanQR = findViewById(R.id.btnScanQR)
        ivResult = findViewById(R.id.ivResult)
        tvScannedText = findViewById(R.id.tvScannedText)
    }

    private fun setupClickListeners() {

        // Open QR Generator - user can input text/emoji/link and select image
        btnGenerateQR.setOnClickListener {
            openQRGenerator()
        }

        // Open QR Scanner - user can upload image or capture from camera
        btnScanQR.setOnClickListener {
            openQRScanner()
        }
    }

    // Simple method to open QR Generator with 2 fields (text and image)
    private fun openQRGenerator() {
        val intent = Intent(this, QRGenerativeActivity::class.java)
        qrGeneratorLauncher.launch(intent)
    }

    // Simple method to open QR Scanner with 2 options (upload and capture)
    private fun openQRScanner() {
        val intent = Intent(this, QRScannerActivity::class.java)
        qrScannerLauncher.launch(intent)
    }
}
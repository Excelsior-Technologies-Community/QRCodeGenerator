package com.ext.codegenerator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ext.qrcodegenerator.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.EnumMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraPreviewActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private var isScanning = true
    private val multiFormatReader = MultiFormatReader()
    private var lastFocusTime = 0L
    private val FOCUS_INTERVAL = 3000L // Focus every 3 seconds

    companion object {
        private const val PERMISSION_REQUEST_CAMERA = 1001
        const val EXTRA_SCANNED_TEXT = "scanned_text"
        const val RESULT_QR_SCANNED = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)
        
        previewView = findViewById(R.id.preview_view)
        setupPreviewView()
        configureReader()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CAMERA
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupPreviewView() {
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }

    private fun configureReader() {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.POSSIBLE_FORMATS, listOf(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.AZTEC,
                BarcodeFormat.PDF_417
            ))
            put(DecodeHintType.TRY_HARDER, true)
            put(DecodeHintType.CHARACTER_SET, "UTF-8")
            put(DecodeHintType.PURE_BARCODE, true)
            put(DecodeHintType.ASSUME_GS1, true)
        }
        multiFormatReader.setHints(hints)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                if (!isScanning) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val buffer = imageProxy.planes[0].buffer
                val data = buffer.toByteArray()
                val width = imageProxy.width
                val height = imageProxy.height

                try {
                    val source = PlanarYUVLuminanceSource(
                        data,
                        width,
                        height,
                        0,
                        0,
                        width,
                        height,
                        false
                    )

                    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                    try {
                        val result: Result = multiFormatReader.decodeWithState(binaryBitmap)
                        
                        if (!result.text.isNullOrEmpty()) {
                            isScanning = false
                            runOnUiThread {
                                handleScanResult(result)
                            }
                        }
                    } catch (e: Exception) {
                        // Continue scanning
                    } finally {
                        multiFormatReader.reset()
                    }
                } catch (e: Exception) {
                    // Handle any errors silently to avoid flooding logs
                } finally {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )

                // Configure camera for screen scanning
                camera.cameraControl.apply {
                    // Disable flash for screen scanning
                    enableTorch(false)
                    
                    // Set minimum zoom for better screen capture
                    setLinearZoom(0.1f)

                    // Initial focus
                    startFocusing(camera)
                }

                // Setup tap to focus
                previewView.setOnTouchListener { _, event ->
                    val factory = SurfaceOrientedMeteringPointFactory(
                        previewView.width.toFloat(),
                        previewView.height.toFloat()
                    )
                    val autoFocusPoint = factory.createPoint(event.x, event.y)
                    try {
                        camera.cameraControl.startFocusAndMetering(
                            FocusMeteringAction.Builder(autoFocusPoint)
                                .setAutoCancelDuration(5, TimeUnit.SECONDS)
                                .build()
                        )
                    } catch (e: Exception) {
                        // Handle focus error silently
                    }
                    true
                }

            } catch (e: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startFocusing(camera: androidx.camera.core.Camera) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFocusTime > FOCUS_INTERVAL) {
            lastFocusTime = currentTime
            try {
                val factory = SurfaceOrientedMeteringPointFactory(
                    previewView.width.toFloat(),
                    previewView.height.toFloat()
                )
                val centerPoint = factory.createPoint(
                    previewView.width / 2f,
                    previewView.height / 2f
                )
                
                camera.cameraControl.startFocusAndMetering(
                    FocusMeteringAction.Builder(centerPoint)
                        .setAutoCancelDuration(2, TimeUnit.SECONDS)
                        .build()
                )
            } catch (e: Exception) {
                // Handle focus error silently
            }
        }
    }

    private fun handleScanResult(result: Result) {
        val scannedText = result.text
        if (scannedText.isNotEmpty()) {
            val resultIntent = Intent().apply {
                putExtra(EXTRA_SCANNED_TEXT, scannedText)
            }
            setResult(RESULT_QR_SCANNED, resultIntent)
            finish()
        } else {
            Toast.makeText(this, "Invalid QR code content", Toast.LENGTH_SHORT).show()
            isScanning = true
        }
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
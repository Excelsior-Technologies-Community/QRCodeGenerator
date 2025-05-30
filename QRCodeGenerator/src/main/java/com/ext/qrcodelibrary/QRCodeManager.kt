package com.ext.qrcodelibrary

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.*
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRCodeManager(private val context: Context) {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var qrCodeScanCallback: ((String) -> Unit)? = null

    fun generateQRCode(
        content: String,
        size: Int,
        centerImage: Bitmap? = null,
        centerImageSize: Float = 0.2f
    ): Bitmap {
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 2)
        }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Convert BitMatrix to Bitmap
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        // Add center image if provided
        centerImage?.let {
            val centerImageWidth = (width * centerImageSize).toInt()
            val centerImageHeight = (height * centerImageSize).toInt()
            val scaledCenterImage = Bitmap.createScaledBitmap(it, centerImageWidth, centerImageHeight, true)
            
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val left = (width - centerImageWidth) / 2f
            val top = (height - centerImageHeight) / 2f
            
            canvas.drawBitmap(scaledCenterImage, left, top, paint)
        }

        return bitmap
    }

    fun startQRCodeScanning(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        scanAreaSize: Float = 0.8f,
        callback: (String) -> Unit
    ) {
        qrCodeScanCallback = callback
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy, scanAreaSize)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun scanQRFromImage(uri: Uri): String? {
        try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return null

            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()

            return try {
                reader.decode(binaryBitmap).text
            } catch (e: Exception) {
                null
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy, scanAreaSize: Float) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            // Process the image looking for QR codes
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        if (barcode.format == Barcode.FORMAT_QR_CODE) {
                            barcode.rawValue?.let { qrContent ->
                                qrCodeScanCallback?.invoke(qrContent)
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    fun stopScanning() {
        cameraExecutor.shutdown()
    }
} 
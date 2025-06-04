package com.ext.qrcodelibrary

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.ext.qrcodelibrary.utils.ImageUtils
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.*
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRCodeManager(private val context: Context) {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var qrCodeScanCallback: ((String) -> Unit)? = null

    companion object {
        private const val QR_CODE_SIZE = 512
    }

    data class QRConfig(
        val content: String,
        val size: Int = QR_CODE_SIZE,
        val centerImage: Bitmap? = null,
        val centerImageSize: Float = 0.2f, // Percentage of QR code size
        val qrColor: Int = Color.BLACK,
        val backgroundColor: Int = Color.WHITE,
        val errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.H,
        val circleBorderColor: Int = Color.WHITE,
        val circleBorderWidth: Float = 4f,
        val addGlow: Boolean = false,
        val glowColor: Int = Color.WHITE,
        val glowRadius: Float = 10f
    )

    fun generateQRCode(config: QRConfig): Bitmap {
        val hints = mutableMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, config.errorCorrectionLevel)
            put(EncodeHintType.MARGIN, 1)
        }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(
            config.content,
            BarcodeFormat.QR_CODE,
            config.size,
            config.size,
            hints
        )

        val qrBitmap = Bitmap.createBitmap(config.size, config.size, Bitmap.Config.ARGB_8888)
        for (x in 0 until config.size) {
            for (y in 0 until config.size) {
                qrBitmap.setPixel(x, y, if (bitMatrix[x, y]) config.qrColor else config.backgroundColor)
            }
        }

        // Add center image if provided
        config.centerImage?.let { originalImage ->
            val centerSize = (config.size * config.centerImageSize).toInt()
            val scaledImage = Bitmap.createScaledBitmap(originalImage, centerSize, centerSize, true)
            
            // Create circular image with border
            val circularImage = ImageUtils.createCircularBitmap(
                scaledImage,
                config.circleBorderColor,
                config.circleBorderWidth
            )

            // Calculate center position
            val left = (config.size - centerSize) / 2
            val top = (config.size - centerSize) / 2

            // Draw the circular image onto the QR code
            val canvas = android.graphics.Canvas(qrBitmap)
            canvas.drawBitmap(circularImage, left.toFloat(), top.toFloat(), null)
        }

        // Add glow effect if enabled
        return if (config.addGlow) {
            ImageUtils.addGlowEffect(qrBitmap, config.glowColor, config.glowRadius)
        } else {
            qrBitmap
        }
    }

    @ExperimentalGetImage
    fun startQRCodeScanning(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        scanAreaSize: Float = 0.8f,
        callback: (String) -> Unit,
        onCameraReady: (androidx.camera.core.Camera) -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(
                            ContextCompat.getMainExecutor(context),
                            QRCodeAnalyzer(scanAreaSize) { result ->
                                callback(result)
                            }
                        )
                    }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
                onCameraReady(camera)
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

    @OptIn(ExperimentalGetImage::class) private fun processImageProxy(imageProxy: ImageProxy, scanAreaSize: Float) {
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

    fun saveQRCodeToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(imageCollection, contentValues) ?: return null

        try {
            contentResolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw Exception("Failed to save bitmap")
                }
            } ?: throw Exception("Failed to open output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }

            return uri
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            return null
        }
    }
} 
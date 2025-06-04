package com.ext.qrcodelibrary

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.camera.core.Camera
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.TorchState
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.ext.qrcodelibrary.databinding.ViewQrScannerBinding

class QRScannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewQrScannerBinding = ViewQrScannerBinding.inflate(
        LayoutInflater.from(context), this
    )
    private var qrCodeManager: QRCodeManager = QRCodeManager(context)
    private var camera: Camera? = null
    private var isFlashOn = false

    init {
        loadAttributes(attrs)
        setupFlashButton()
    }

    private fun loadAttributes(attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.QRScannerView,
            0, 0
        ).apply {
            try {
                val frameColor = getColor(R.styleable.QRScannerView_scanner_frame_color, -0x1)
                val frameWidth = getDimension(R.styleable.QRScannerView_scanner_frame_width, 250f)
                val frameHeight = getDimension(R.styleable.QRScannerView_scanner_frame_height, 250f)
                val cornerRadius = getDimension(R.styleable.QRScannerView_scanner_frame_corner_radius, 8f)
                val strokeWidth = getDimension(R.styleable.QRScannerView_scanner_frame_stroke_width, 2f)
                val overlayColor = getColor(R.styleable.QRScannerView_scanner_overlay_color, 0x33000000)

                binding.scannerFrame.apply {
                    layoutParams.width = frameWidth.toInt()
                    layoutParams.height = frameHeight.toInt()
                    background = context.getDrawable(R.drawable.scanner_frame)?.apply {
                        setTint(frameColor)
                    }
                }

                binding.scannerOverlay.setBackgroundColor(overlayColor)
            } finally {
                recycle()
            }
        }
    }

    private fun setupFlashButton() {
        binding.flashToggleButton.setOnClickListener {
            toggleFlash()
        }
        // Initially hide the flash button until we confirm flash is available
        binding.flashToggleButton.visibility = GONE
    }

    private fun toggleFlash() {
        camera?.let { camera ->
            isFlashOn = !isFlashOn
            camera.cameraControl.enableTorch(isFlashOn)
            updateFlashIcon()
        }
    }

    private fun updateFlashIcon() {
        binding.flashToggleButton.setImageResource(
            if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        )
    }

    @ExperimentalGetImage
    fun startScanning(lifecycleOwner: LifecycleOwner, callback: (String) -> Unit) {
        qrCodeManager.startQRCodeScanning(
            previewView = binding.previewView,
            lifecycleOwner = lifecycleOwner,
            scanAreaSize = 0.8f,
            callback = callback,
            onCameraReady = { cam ->
                camera = cam
                // Show flash button only if flash is available
                cam.cameraInfo.hasFlashUnit().let { hasFlash ->
                    binding.flashToggleButton.visibility = if (hasFlash) VISIBLE else GONE
                }
                // Observe torch state
                cam.cameraInfo.torchState.observe(lifecycleOwner) { torchState ->
                    isFlashOn = torchState == TorchState.ON
                    updateFlashIcon()
                }
            }
        )
    }

    fun stopScanning() {
        camera?.cameraControl?.enableTorch(false)
        qrCodeManager.stopScanning()
        camera = null
    }

    fun getPreviewView(): PreviewView = binding.previewView
} 
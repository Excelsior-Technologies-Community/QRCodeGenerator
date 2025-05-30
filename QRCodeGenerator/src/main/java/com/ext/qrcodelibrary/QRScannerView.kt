package com.ext.qrcodelibrary

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
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

    init {
        loadAttributes(attrs)
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

    fun startScanning(lifecycleOwner: LifecycleOwner, callback: (String) -> Unit) {
        qrCodeManager.startQRCodeScanning(
            previewView = binding.previewView,
            lifecycleOwner = lifecycleOwner,
            scanAreaSize = 0.8f,
            callback = callback
        )
    }

    fun stopScanning() {
        qrCodeManager.stopScanning()
    }

    fun getPreviewView(): PreviewView = binding.previewView
} 
package com.ext.qrcodelibrary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.ext.qrcodelibrary.databinding.ViewQrCodeBinding
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QRCodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewQrCodeBinding =
        ViewQrCodeBinding.inflate(LayoutInflater.from(context), this, true)
    private var qrCodeManager: QRCodeManager = QRCodeManager(context)
    private var selectedImage: Bitmap? = null
    private var generatedQRCode: Bitmap? = null
    private var currentText: String = ""

    fun setText(text: String) {
        currentText = text
        generateQRCode(text)
    }

    fun setImage(bitmap: Bitmap) {
        selectedImage = bitmap
        generateQRCode(currentText)
    }

    fun generateQRCode() {
        generateQRCode(currentText)
    }

    private fun generateQRCode(content: String) {
        if (content.isNotEmpty()) {
            val config = QRCodeManager.QRConfig(
                content = content,
                size = 512,
                centerImage = selectedImage,
                centerImageSize = 0.2f,
                qrColor = Color.BLACK,
                backgroundColor = Color.WHITE,
                errorCorrectionLevel = ErrorCorrectionLevel.H,
                circleBorderColor = Color.WHITE,
                circleBorderWidth = 4f,
                addGlow = false
            )
            
            generatedQRCode = qrCodeManager.generateQRCode(config)
            binding.qrCodeImageView.setImageBitmap(generatedQRCode)
        }
    }

    fun getQRCodeBitmap(): Bitmap? = generatedQRCode
} 
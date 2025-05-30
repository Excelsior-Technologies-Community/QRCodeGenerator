package com.ext.qrcodelibrary

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
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

    private var onImagePickerRequestListener: (() -> Unit)? = null
    private var onQRGeneratedListener: ((Bitmap) -> Unit)? = null
    private var onImageSelectedListener: ((Bitmap) -> Unit)? = null
    private var onDownloadClickListener: ((Bitmap) -> Unit)? = null

    init {
        setupViews()
        loadAttributes(attrs)
    }

    private fun setupViews() {
        binding.selectImageButton.setOnClickListener {
            onImagePickerRequestListener?.invoke()
        }

        binding.generateButton.setOnClickListener {
            generateQRCode()
        }

        binding.downloadButton.setOnClickListener {
            generatedQRCode?.let { qrCode ->
                onDownloadClickListener?.invoke(qrCode)
            }
        }
    }

    private fun loadAttributes(attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.QRCodeView,
            0, 0
        ).apply {
            try {
                binding.titleText.text = getString(R.styleable.QRCodeView_qr_title) ?: "QR Code Generator"
                binding.titleText.textSize = getDimension(R.styleable.QRCodeView_qr_titleTextSize, 24f)
                binding.titleText.setTextColor(getColor(R.styleable.QRCodeView_qr_titleTextColor, -0x1000000))
                binding.titleText.visibility = if (getBoolean(R.styleable.QRCodeView_qr_showTitle, true)) View.VISIBLE else View.GONE

                binding.inputText.hint = getString(R.styleable.QRCodeView_qr_inputHint) ?: "Enter text"
                binding.inputText.maxLines = getInteger(R.styleable.QRCodeView_qr_inputMaxLines, 5)

                binding.selectImageButton.text = getString(R.styleable.QRCodeView_qr_selectImageButtonText) ?: "Select Image"
                binding.generateButton.text = getString(R.styleable.QRCodeView_qr_generateButtonText) ?: "Generate QR"
                binding.downloadButton.text = getString(R.styleable.QRCodeView_qr_downloadButtonText) ?: "Save QR"

                binding.selectImageButton.visibility = if (getBoolean(R.styleable.QRCodeView_qr_showImageSelection, true)) View.VISIBLE else View.GONE
                binding.downloadButton.visibility = if (getBoolean(R.styleable.QRCodeView_qr_showDownloadButton, true)) View.VISIBLE else View.GONE
            } finally {
                recycle()
            }
        }
    }

    private fun generateQRCode() {
        val content = binding.inputText.text.toString()
        if (content.isNotEmpty()) {
            val config = QRCodeManager.QRConfig(
                content = content,
                size = 512,
                centerImage = selectedImage,
                centerImageSize = 0.2f,
                errorCorrectionLevel = ErrorCorrectionLevel.H
            )
            val qrCode = qrCodeManager.generateQRCode(config)
            generatedQRCode = qrCode
            binding.qrCodeImageView.setImageBitmap(qrCode)
            onQRGeneratedListener?.invoke(qrCode)
        }
    }

    fun setText(text: String) {
        binding.inputText.setText(text)
    }

    fun setImage(bitmap: Bitmap) {
        selectedImage = bitmap
        binding.selectedImageView.setImageBitmap(bitmap)
        binding.selectedImageView.visibility = View.VISIBLE
        onImageSelectedListener?.invoke(bitmap)
    }

    fun getGeneratedQRCode(): Bitmap? = generatedQRCode

    fun setOnImagePickerRequestListener(listener: () -> Unit) {
        onImagePickerRequestListener = listener
    }

    fun setOnQRGeneratedListener(listener: (Bitmap) -> Unit) {
        onQRGeneratedListener = listener
    }

    fun setOnImageSelectedListener(listener: (Bitmap) -> Unit) {
        onImageSelectedListener = listener
    }

    fun setOnDownloadClickListener(listener: (Bitmap) -> Unit) {
        onDownloadClickListener = listener
    }
} 
package com.ext.codegenerator

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.ext.codegenerator.R
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.qrcodegen.library.QRCodeConfig
import java.io.IOException
import java.util.*

class QRCodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // UI Components
    private lateinit var titleTextView: TextView
    private lateinit var inputLayout: TextInputLayout
    private lateinit var inputEditText: TextInputEditText
    private lateinit var imageSelectionLayout: LinearLayout
    private lateinit var imageDescriptionText: TextView
    private lateinit var selectImageButton: Button
    private lateinit var selectedImageView: ImageView
    private lateinit var generateButton: Button
    private lateinit var qrCardView: CardView
    private lateinit var qrImageView: ImageView
    private lateinit var downloadButton: Button


    // Configuration
    private var config = QRCodeConfig()

    // Data
    private var selectedImageBitmap: Bitmap? = null
    private var generatedQrBitmap: Bitmap? = null

    // Callbacks
    private var onQRGeneratedListener: ((Bitmap) -> Unit)? = null
    private var onImageSelectedListener: ((Bitmap) -> Unit)? = null
    private var onDownloadClickListener: ((Bitmap) -> Unit)? = null

    init {
        initializeView(attrs)
    }

    private fun initializeView(attrs: AttributeSet?) {
        LayoutInflater.from(context).inflate(R.layout.qr_code_view, this, true)

        // Initialize views
        titleTextView = findViewById(R.id.tv_title)
        inputLayout = findViewById(R.id.til_input)
        inputEditText = findViewById(R.id.et_input)
        imageSelectionLayout = findViewById(R.id.ll_image_selection)
        imageDescriptionText = findViewById(R.id.tv_image_description)
        selectImageButton = findViewById(R.id.btnSelectImage)
        selectedImageView = findViewById(R.id.iv_selected_image)
        generateButton = findViewById(R.id.btnGenerate)
        qrCardView = findViewById(R.id.cvQrCode)
        qrImageView = findViewById(R.id.ivQrCode)
        downloadButton = findViewById(R.id.btnDownload)

        // Parse custom attributes
        parseAttributes(attrs)

        // Apply configuration
        applyConfiguration()

        // Setup listeners
        setupClickListeners()
    }

    private fun parseAttributes(attrs: AttributeSet?) {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.QRCodeView)

            try {
                // Title configuration
                config.titleText = typedArray.getString(R.styleable.QRCodeView_qr_title) ?: config.titleText
                config.titleTextSize = typedArray.getDimensionPixelSize(R.styleable.QRCodeView_qr_titleTextSize, config.titleTextSize.toInt()).toFloat()
                config.titleTextColor = typedArray.getColor(R.styleable.QRCodeView_qr_titleTextColor, config.titleTextColor)

                // Input configuration
                config.inputHint = typedArray.getString(R.styleable.QRCodeView_qr_inputHint) ?: config.inputHint
                config.inputMaxLines = typedArray.getInt(R.styleable.QRCodeView_qr_inputMaxLines, config.inputMaxLines)

                // Button texts
                config.selectImageButtonText = typedArray.getString(R.styleable.QRCodeView_qr_selectImageButtonText) ?: config.selectImageButtonText
                config.generateButtonText = typedArray.getString(R.styleable.QRCodeView_qr_generateButtonText) ?: config.generateButtonText
                config.downloadButtonText = typedArray.getString(R.styleable.QRCodeView_qr_downloadButtonText) ?: config.downloadButtonText

                // Image description
                config.imageDescriptionText = typedArray.getString(R.styleable.QRCodeView_qr_imageDescriptionText) ?: config.imageDescriptionText

                // QR Code configuration
                config.qrCodeSize = typedArray.getDimensionPixelSize(R.styleable.QRCodeView_qr_codeSize, config.qrCodeSize)
                config.qrBackgroundColor = typedArray.getColor(R.styleable.QRCodeView_qr_backgroundColor, config.qrBackgroundColor)
                config.qrForegroundColor = typedArray.getColor(R.styleable.QRCodeView_qr_foregroundColor, config.qrForegroundColor)

                // Visibility options
                config.showTitle = typedArray.getBoolean(R.styleable.QRCodeView_qr_showTitle, config.showTitle)
                config.showImageSelection = typedArray.getBoolean(R.styleable.QRCodeView_qr_showImageSelection, config.showImageSelection)
                config.showDownloadButton = typedArray.getBoolean(R.styleable.QRCodeView_qr_showDownloadButton, config.showDownloadButton)

                // Overlay configuration
                config.overlaySize = typedArray.getFloat(R.styleable.QRCodeView_qr_overlaySize, config.overlaySize)
                config.overlayBorderColor = typedArray.getColor(R.styleable.QRCodeView_qr_overlayBorderColor, config.overlayBorderColor)
                config.overlayBorderWidth = typedArray.getFloat(R.styleable.QRCodeView_qr_overlayBorderWidth, config.overlayBorderWidth)
                config.overlayBackgroundColor = typedArray.getColor(R.styleable.QRCodeView_qr_overlayBackgroundColor, config.overlayBackgroundColor)

            } finally {
                typedArray.recycle()
            }
        }
    }

    private fun applyConfiguration() {
        // Apply title configuration
        titleTextView.text = config.titleText
        titleTextView.textSize = config.titleTextSize / resources.displayMetrics.scaledDensity
        titleTextView.setTextColor(config.titleTextColor)
        titleTextView.visibility = if (config.showTitle) View.VISIBLE else View.GONE

        // Apply input configuration
        inputLayout.hint = config.inputHint
        inputEditText.maxLines = config.inputMaxLines

        // Apply button texts
        selectImageButton.text = config.selectImageButtonText
        generateButton.text = config.generateButtonText
        downloadButton.text = config.downloadButtonText

        // Apply image description
        imageDescriptionText.text = config.imageDescriptionText

        // Apply visibility
        imageSelectionLayout.visibility = if (config.showImageSelection) View.VISIBLE else View.GONE
        downloadButton.visibility = if (config.showDownloadButton) View.VISIBLE else View.GONE

        // Apply QR card size
        val layoutParams = qrCardView.layoutParams
        layoutParams.width = config.qrCodeSize
        layoutParams.height = config.qrCodeSize
        qrCardView.layoutParams = layoutParams
    }

    private fun setupClickListeners() {
        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        generateButton.setOnClickListener {
            generateQRCode()
        }

        downloadButton.setOnClickListener {
            generatedQrBitmap?.let { bitmap ->
                onDownloadClickListener?.invoke(bitmap) ?: defaultDownloadHandler(bitmap)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        // Note: This requires activity context to handle result
        // Users should handle this in their activity
        onImageSelectedListener?.invoke(selectedImageBitmap ?: return)
    }

    fun setImageFromUri(uri: Uri) {
        try {
            selectedImageBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            selectedImageView.setImageBitmap(selectedImageBitmap)
            selectedImageView.visibility = View.VISIBLE
            onImageSelectedListener?.invoke(selectedImageBitmap!!)
        } catch (e: IOException) {
            Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
        }
    }

    fun setImageFromBitmap(bitmap: Bitmap) {
        selectedImageBitmap = bitmap
        selectedImageView.setImageBitmap(bitmap)
        selectedImageView.visibility = View.VISIBLE
        onImageSelectedListener?.invoke(bitmap)
    }

    private fun generateQRCode() {
        val inputText = inputEditText.text.toString().trim()

        if (inputText.isEmpty()) {
            Toast.makeText(context, "Please enter text", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val qrBitmap = createQRCodeBitmap(inputText, config.qrCodeSize, config.qrCodeSize)

            generatedQrBitmap = if (selectedImageBitmap != null && config.showImageSelection) {
                overlayImageOnQR(qrBitmap, selectedImageBitmap!!)
            } else {
                qrBitmap
            }

            qrImageView.setImageBitmap(generatedQrBitmap)
            qrCardView.visibility = View.VISIBLE
            if (config.showDownloadButton) {
                downloadButton.visibility = View.VISIBLE
            }

            onQRGeneratedListener?.invoke(generatedQrBitmap!!)

        } catch (e: Exception) {
            Toast.makeText(context, "Error generating QR Code: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createQRCodeBitmap(text: String, width: Int, height: Int): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 2)
        }

        try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) config.qrForegroundColor else config.qrBackgroundColor)
                }
            }
            return bitmap

        } catch (e: WriterException) {
            throw RuntimeException("Error generating QR Code", e)
        }
    }

    private fun overlayImageOnQR(qrBitmap: Bitmap, overlayBitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(qrBitmap.width, qrBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        canvas.drawBitmap(qrBitmap, 0f, 0f, null)

        val overlaySize = (qrBitmap.width * config.overlaySize).toInt()
        val left = (qrBitmap.width - overlaySize) / 2
        val top = (qrBitmap.height - overlaySize) / 2

        val scaledOverlay = createCenterCroppedBitmap(overlayBitmap, overlaySize)
        val circularOverlay = getCircularBitmap(scaledOverlay)

        // Background circle
        val backgroundPaint = Paint().apply {
            color = config.overlayBackgroundColor
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = config.overlayBorderColor
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = config.overlayBorderWidth
        }

        val radius = overlaySize / 2f + 12f
        val centerX = left + overlaySize / 2f
        val centerY = top + overlaySize / 2f

        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        canvas.drawCircle(centerX, centerY, radius, borderPaint)
        canvas.drawBitmap(circularOverlay, left.toFloat(), top.toFloat(), null)

        return result
    }

    private fun createCenterCroppedBitmap(source: Bitmap, size: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height
        val xScale = size.toFloat() / sourceWidth
        val yScale = size.toFloat() / sourceHeight
        val scale = maxOf(xScale, yScale)
        val scaledWidth = (size / scale).toInt()
        val scaledHeight = (size / scale).toInt()
        val left = (sourceWidth - scaledWidth) / 2
        val top = (sourceHeight - scaledHeight) / 2

        val sourceRect = Rect(left, top, left + scaledWidth, top + scaledHeight)
        val destRect = Rect(0, 0, size, size)

        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, sourceRect, destRect, Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        })

        return result
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
        }

        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)

        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    private fun defaultDownloadHandler(bitmap: Bitmap) {
        // Default implementation - users can override this
        Toast.makeText(context, "Please implement download functionality", Toast.LENGTH_SHORT).show()
    }

    // Public API methods
    fun setConfiguration(config: QRCodeConfig) {
        this.config = config
        applyConfiguration()
    }

    fun getConfiguration(): QRCodeConfig = config.copy()

    fun setText(text: String) {
        inputEditText.setText(text)
    }

    fun getText(): String = inputEditText.text.toString()

    fun generateQRCodeWithText(text: String) {
        setText(text)
        generateQRCode()
    }

    fun getGeneratedQRCode(): Bitmap? = generatedQrBitmap

    // Listener setters
    fun setOnQRGeneratedListener(listener: (Bitmap) -> Unit) {
        onQRGeneratedListener = listener
    }

    fun setOnImageSelectedListener(listener: (Bitmap) -> Unit) {
        onImageSelectedListener = listener
    }

    fun setOnDownloadClickListener(listener: (Bitmap) -> Unit) {
        onDownloadClickListener = listener
    }

    // Helper method to handle image picker result
    fun handleImagePickerResult(uri: Uri?) {
        uri?.let { setImageFromUri(it) }
    }
}
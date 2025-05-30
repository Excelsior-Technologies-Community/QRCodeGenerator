package com.ext.codegenerator

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * Configuration class for QRCodeView customization
 */
data class QRCodeConfig(
    // Title Configuration
    var titleText: String = "QR Code Generator",
    var titleTextSize: Float = 72f, // in pixels
    @ColorInt var titleTextColor: Int = Color.BLACK,
    var showTitle: Boolean = true,

    // Input Configuration
    var inputHint: String = "Enter text, URL, or emoji",
    var inputMaxLines: Int = 4,

    // Button Configuration
    var selectImageButtonText: String = "Select Image",
    var generateButtonText: String = "Generate QR Code",
    var downloadButtonText: String = "Download QR Code",

    // Image Selection Configuration
    var imageDescriptionText: String = "Add center image (optional):",
    var showImageSelection: Boolean = true,

    // QR Code Configuration
    var qrCodeSize: Int = 900, // in pixels
    @ColorInt var qrBackgroundColor: Int = Color.WHITE,
    @ColorInt var qrForegroundColor: Int = Color.BLACK,

    // Overlay Configuration (for center image)
    var overlaySize: Float = 0.25f, // 25% of QR code size
    @ColorInt var overlayBorderColor: Int = Color.BLACK,
    var overlayBorderWidth: Float = 4f,
    @ColorInt var overlayBackgroundColor: Int = Color.WHITE,

    // Visibility Configuration
    var showDownloadButton: Boolean = true,

    // Error Correction Level
    var errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.HIGH
) {

    enum class ErrorCorrectionLevel {
        LOW,    // ~7% correction
        MEDIUM, // ~15% correction
        QUARTILE, // ~25% correction
        HIGH    // ~30% correction
    }

    /**
     * Builder pattern for easy configuration
     */
    class Builder {
        private val config = QRCodeConfig()

        fun setTitle(title: String, textSize: Float = 72f, textColor: Int = Color.BLACK, show: Boolean = true) = apply {
            config.titleText = title
            config.titleTextSize = textSize
            config.titleTextColor = textColor
            config.showTitle = show
        }

        fun setInputHint(hint: String, maxLines: Int = 4) = apply {
            config.inputHint = hint
            config.inputMaxLines = maxLines
        }

        fun setButtonTexts(
            selectImage: String = "Select Image",
            generate: String = "Generate QR Code",
            download: String = "Download QR Code"
        ) = apply {
            config.selectImageButtonText = selectImage
            config.generateButtonText = generate
            config.downloadButtonText = download
        }

        fun setQRCodeAppearance(
            size: Int = 900,
            backgroundColor: Int = Color.WHITE,
            foregroundColor: Int = Color.BLACK
        ) = apply {
            config.qrCodeSize = size
            config.qrBackgroundColor = backgroundColor
            config.qrForegroundColor = foregroundColor
        }

        fun setOverlayConfiguration(
            size: Float = 0.25f,
            borderColor: Int = Color.BLACK,
            borderWidth: Float = 4f,
            backgroundColor: Int = Color.WHITE
        ) = apply {
            config.overlaySize = size
            config.overlayBorderColor = borderColor
            config.overlayBorderWidth = borderWidth
            config.overlayBackgroundColor = backgroundColor
        }

        fun setVisibility(
            showTitle: Boolean = true,
            showImageSelection: Boolean = true,
            showDownloadButton: Boolean = true
        ) = apply {
            config.showTitle = showTitle
            config.showImageSelection = showImageSelection
            config.showDownloadButton = showDownloadButton
        }

        fun setErrorCorrectionLevel(level: ErrorCorrectionLevel) = apply {
            config.errorCorrectionLevel = level
        }

        fun build(): QRCodeConfig = config.copy()
    }

    companion object {
        /**
         * Creates a minimal QR code configuration with only essential features
         */
        fun minimal(): QRCodeConfig {
            return QRCodeConfig(
                showTitle = false,
                showImageSelection = false,
                showDownloadButton = false
            )
        }

        /**
         * Creates a compact QR code configuration for small screens
         */
        fun compact(): QRCodeConfig {
            return QRCodeConfig(
                titleTextSize = 56f,
                qrCodeSize = 600,
                inputMaxLines = 2
            )
        }

        /**
         * Creates a QR code configuration for dark theme
         */
        fun darkTheme(): QRCodeConfig {
            return QRCodeConfig(
                titleTextColor = Color.WHITE,
                qrBackgroundColor = Color.BLACK,
                qrForegroundColor = Color.WHITE,
                overlayBackgroundColor = Color.BLACK,
                overlayBorderColor = Color.WHITE
            )
        }

        /**
         * Creates a QR code configuration with custom brand colors
         */
        fun branded(
            primaryColor: Int,
            secondaryColor: Int = Color.WHITE,
            accentColor: Int = primaryColor
        ): QRCodeConfig {
            return QRCodeConfig(
                titleTextColor = primaryColor,
                qrForegroundColor = primaryColor,
                qrBackgroundColor = secondaryColor,
                overlayBorderColor = accentColor
            )
        }
    }
}
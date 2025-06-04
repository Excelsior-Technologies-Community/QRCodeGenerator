# QRCodeGenerator

A powerful and feature-rich Android library for generating and scanning QR codes with advanced customization options.

## Features

- 🎨 **Customizable QR Code Generation**
  - Custom colors for QR code and background
  - Center image support with circular border
  - Glow effect option
  - Adjustable error correction levels
  - Customizable size

- 📱 **QR Code Scanning**
  - Real-time QR code scanning using CameraX
  - Support for scanning QR codes from images
  - Customizable scan area
  - ML Kit integration for improved accuracy

- 💾 **Storage & Export**
  - Save QR codes to device gallery
  - High-quality PNG export
  - Automatic file naming

## Installation

Add the following to your project's `build.gradle` file:

```gradle
dependencies {
    implementation 'com.ext:qrcodelibrary:1.0.0'
}
```

## Usage

### Generating QR Codes

```kotlin
val qrCodeManager = QRCodeManager(context)

// Basic QR Code
val basicConfig = QRCodeManager.QRConfig(
    content = "https://example.com"
)
val basicQRCode = qrCodeManager.generateQRCode(basicConfig)

// Advanced QR Code with customization
val advancedConfig = QRCodeManager.QRConfig(
    content = "https://example.com",
    size = 512,
    centerImage = yourLogoBitmap,
    centerImageSize = 0.2f,
    qrColor = Color.BLACK,
    backgroundColor = Color.WHITE,
    errorCorrectionLevel = ErrorCorrectionLevel.H,
    circleBorderColor = Color.WHITE,
    circleBorderWidth = 4f,
    addGlow = true,
    glowColor = Color.WHITE,
    glowRadius = 10f
)
val advancedQRCode = qrCodeManager.generateQRCode(advancedConfig)

// Save to gallery
qrCodeManager.saveQRCodeToGallery(context, advancedQRCode, "my_qr_code.png")
```

### Scanning QR Codes

```kotlin
// Real-time scanning
qrCodeManager.startQRCodeScanning(
    previewView = previewView,
    lifecycleOwner = this,
    scanAreaSize = 0.8f,
    callback = { result ->
        // Handle scanned QR code result
        Log.d("QRCode", "Scanned: $result")
    },
    onCameraReady = { camera ->
        // Camera is ready
    }
)

// Scan from image
val uri = // Your image URI
val result = qrCodeManager.scanQRFromImage(uri)
```

## Requirements

- Android API Level 26+
- Kotlin 1.8+
- AndroidX

## Dependencies

The library uses the following dependencies:
- ZXing for QR code generation
- ML Kit for barcode scanning
- CameraX for camera functionality
- AndroidX Core and AppCompat

## Permissions

Add the following permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28" />
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Author

[Your Name]

## Support

If you encounter any issues or have questions, please open an issue in the GitHub repository.

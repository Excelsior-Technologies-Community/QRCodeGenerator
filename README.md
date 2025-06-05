# QRCodeGenerator

A powerful and feature-rich Android library for generating and scanning QR codes with advanced customization options.

## Gradle

```gradle
dependencies {
    implementation 'com.ext:qrcodelibrary:1.0.0'
}
```

## Usage

### Basic QR Code Generation

```kotlin
val qrCodeManager = QRCodeManager(context)

// Create basic configuration
val basicConfig = QRCodeManager.QRConfig(
    content = "https://example.com"
)

// Generate QR code
val qrBitmap = qrCodeManager.generateQRCode(basicConfig)
```

### Customized QR Code

```kotlin
val customConfig = QRCodeManager.QRConfig(
    content = "https://example.com",
    size = 512,
    qrColor = Color.BLUE,           // Custom QR code color
    backgroundColor = Color.WHITE,   // Custom background color
    errorCorrectionLevel = ErrorCorrectionLevel.H  // Higher error correction
)
```

### QR Code with Center Image

```kotlin
val configWithImage = QRCodeManager.QRConfig(
    content = "https://example.com",
    size = 512,
    centerImage = yourBitmap,        // Your center image
    centerImageSize = 0.2f,          // Image size (20% of QR code)
    circleBorderColor = Color.WHITE, // Border color around image
    circleBorderWidth = 4f           // Border width in pixels
)
```

### QR Code with Glow Effect

```kotlin
val glowConfig = QRCodeManager.QRConfig(
    content = "https://example.com",
    size = 512,
    addGlow = true,
    glowColor = Color.BLUE,
    glowRadius = 8f
)
```

### QR Code Scanning

```kotlin
// Initialize scanner
val qrCodeManager = QRCodeManager(context)

// Start scanning
qrCodeManager.startQRCodeScanning(
    previewView = previewView,       // CameraX PreviewView in your layout
    lifecycleOwner = lifecycleOwner, // Activity or Fragment
    scanAreaSize = 0.8f             // Scanner area (80% of screen)
) { result ->
    // Handle scanned result
    handleQRContent(result)
}

// Don't forget to stop scanning when done
override fun onDestroy() {
    super.onDestroy()
    qrCodeManager.stopScanning()
}
```

### Required XML for Scanner

```xml
<androidx.camera.view.PreviewView
    android:id="@+id/previewView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### Image-based QR Scanning

```kotlin
// Scan from Uri (Gallery image)
val result = qrCodeManager.scanQRFromImage(uri)
result?.let {
    // Handle scanned content
}
```

### Saving QR Codes

```kotlin
// Save to gallery with custom name
val uri = qrCodeManager.saveQRCodeToGallery(
    context = context,
    bitmap = qrBitmap,
    fileName = "MyQRCode.png"
)

// Handle result
uri?.let {
    // QR code saved successfully
    // Use uri for sharing or further processing
}
```

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

## FAQ

**How can I add a custom center image to my QR code?**
You can use the `centerImage` parameter in `QRConfig` to add any bitmap as a center image. The image will be automatically scaled and centered in the QR code.

**What's the best error correction level to use?**
For most use cases, `ErrorCorrectionLevel.H` (High) is recommended as it provides the best error correction capability, especially when adding a center image.

**How can I handle scanning failures?**
The library includes built-in error handling. For real-time scanning, the callback will only be triggered when a valid QR code is detected. For image scanning, the result will be null if scanning fails.

## License

```
Copyright 2025 Excelsior Technologies

```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

If you encounter any issues or have questions, please open an issue in the GitHub repository.

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

### QR Code Generation with QRCodeView

```kotlin
// In your Activity/Fragment
val qrCodeView = findViewById<QRCodeView>(R.id.qrCodeView)

// Set QR code content
qrCodeView.setContent("https://example.com")

// Customize QR code appearance
qrCodeView.apply {
    setForegroundColor(Color.BLUE)
    setBackgroundColor(Color.WHITE)
    setCodeSize(512)
    setOverlaySize(0.2f)
    setOverlayBorderColor(Color.WHITE)
    setOverlayBorderWidth(4f)
}

// Add center image
qrCodeView.setCenterImage(bitmap)

// Generate QR code
qrCodeView.generateQRCode()

// Get generated QR code bitmap
val qrBitmap = qrCodeView.getQRCodeBitmap()

// Save QR code
qrCodeView.saveQRCode("MyQRCode.png") { uri ->
    // Handle saved QR code URI
}
```

### QR Code Scanning with QRScannerView

```kotlin
// In your Activity/Fragment
val qrScannerView = findViewById<QRScannerView>(R.id.qrScannerView)

// Initialize scanner
qrScannerView.initialize(lifecycleOwner)

// Set scan result callback
qrScannerView.setOnQRCodeScannedListener { result ->
    // Handle scanned result
    handleQRContent(result)
}

// Start scanning
qrScannerView.startScanning()

// Stop scanning
qrScannerView.stopScanning()

// Customize scanner appearance
qrScannerView.apply {
    setFrameColor(Color.WHITE)
    setFrameSize(250f)
    setFrameCornerRadius(8f)
    setFrameStrokeWidth(2f)
    setOverlayColor(Color.parseColor("#33000000"))
}

// Handle permissions
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    qrScannerView.handlePermissionResult(requestCode, permissions, grantResults)
}
```

### Required Permissions

Add these permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28" />
```

### Permission Handling

```kotlin
// In your Activity/Fragment
private fun checkAndRequestPermissions() {
    if (qrScannerView.hasRequiredPermissions()) {
        qrScannerView.startScanning()
    } else {
        qrScannerView.requestPermissions(this)
    }
}
```

### Error Handling

```kotlin
// QR Generation Errors
qrCodeView.setOnErrorListener { error ->
    when (error) {
        QRCodeError.INVALID_CONTENT -> // Handle invalid content
        QRCodeError.SAVE_FAILED -> // Handle save failure
        QRCodeError.IMAGE_TOO_LARGE -> // Handle large image
    }
}

// QR Scanning Errors
qrScannerView.setOnErrorListener { error ->
    when (error) {
        QRScannerError.CAMERA_PERMISSION_DENIED -> // Handle permission denied
        QRScannerError.CAMERA_INITIALIZATION_FAILED -> // Handle camera init failure
        QRScannerError.SCAN_FAILED -> // Handle scan failure
    }
}
```

### Additional Features

#### QR Code Generation
- Support for different QR code formats (URL, Text, Contact, etc.)
- Custom error correction levels
- Glow effect support
- Multiple QR code styles
- Batch QR code generation

#### QR Code Scanning
- Support for multiple barcode formats (QR, Data Matrix, etc.)
- Continuous scanning mode
- Flashlight control
- Auto-focus control
- Scan history
- Batch scanning

### Best Practices

1. **QR Code Generation**
   - Use appropriate error correction level based on content size
   - Optimize center image size to maintain QR code readability
   - Test QR code with different scanning apps
   - Consider adding a logo or branding element

2. **QR Code Scanning**
   - Handle camera permissions properly
   - Implement proper lifecycle management
   - Consider device orientation changes
   - Test on different screen sizes
   - Implement proper error handling

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

## Screenshots

### 1. QR Code Generation with Center Image

![QR Code Generation with Center Image](images/qr_generate_center_image.jpg)

*This screen shows the QR code generation mode. Users can enter text, select a center image (such as a profile photo), and generate a QR code with the image embedded in the center. The generated QR code can be saved to the device.*

---

### 2. QR Code Scanning Mode (Empty)

![QR Code Scanning Mode (Empty)](images/qr_scan_empty.jpg)

*This screen displays the QR code scanning mode before a QR code is detected. The camera preview is active, and the scanning frame is visible, ready to scan any QR code.*

---

### 3. QR Code Scanning Mode (Result Detected)

![QR Code Scanning Mode (Result Detected)](images/qr_scan_result.jpg)

*This screen shows the scanning mode after a QR code has been detected. The scanned content is displayed below the frame, and users can copy or share the result directly from the app.*

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

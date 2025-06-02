# QR Code Generator and Scanner Library

A comprehensive Android library for QR code operations with support for custom designs, real-time scanning, and image processing.

## Installation

Add JitPack repository to your root build.gradle:

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency to your app's build.gradle:

```gradle
dependencies {
    implementation 'com.github.yashraiyani098:QRCodeGenerator:1.4.0'
}
```

## Required Permissions

Add these permissions to your AndroidManifest.xml based on the features you'll use:

```xml
<!-- For QR code scanning -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- For saving QR codes and picking images (Android < 13) -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />

<!-- For Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

## Library Features

### 1. QR Code Generation

#### Basic QR Code
```kotlin
// Initialize manager
val qrCodeManager = QRCodeManager(context)

// Create basic configuration
val basicConfig = QRCodeManager.QRConfig(
    content = "Your content here",
    size = 512  // Size in pixels
)

// Generate QR code
val qrBitmap = qrCodeManager.generateQRCode(basicConfig)
```

#### Customized QR Code
```kotlin
val customConfig = QRCodeManager.QRConfig(
    content = "Your content here",
    size = 512,
    qrColor = Color.BLUE,           // Custom QR code color
    backgroundColor = Color.WHITE,   // Custom background color
    errorCorrectionLevel = ErrorCorrectionLevel.H  // Higher error correction
)
```

#### QR Code with Center Image
```kotlin
val configWithImage = QRCodeManager.QRConfig(
    content = "Your content here",
    size = 512,
    centerImage = yourBitmap,        // Your center image
    centerImageSize = 0.2f,          // Image size (20% of QR code)
    circleBorderColor = Color.WHITE, // Border color around image
    circleBorderWidth = 4f           // Border width in pixels
)
```

#### QR Code with Glow Effect
```kotlin
val glowConfig = QRCodeManager.QRConfig(
    content = "Your content here",
    size = 512,
    addGlow = true,
    glowColor = Color.BLUE,
    glowRadius = 8f
)
```

### 2. QR Code Scanning

#### Camera Scanner Implementation
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

#### Required XML for Scanner
```xml
<androidx.camera.view.PreviewView
    android:id="@+id/previewView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### 3. Image-based QR Scanning

```kotlin
// Scan from Uri (Gallery image)
val result = qrCodeManager.scanQRFromImage(uri)
result?.let {
    // Handle scanned content
}

// Scan from Bitmap
val result = qrCodeManager.scanQRFromImage(bitmap)
result?.let {
    // Handle scanned content
}
```

### 4. Saving QR Codes

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

## Error Handling

The library includes built-in error handling for common scenarios:

```kotlin
try {
    val qrBitmap = qrCodeManager.generateQRCode(config)
} catch (e: IllegalArgumentException) {
    // Handle invalid configuration
} catch (e: Exception) {
    // Handle other errors
}

// For scanning
qrCodeManager.startQRCodeScanning(
    previewView = previewView,
    lifecycleOwner = lifecycleOwner
) { result ->
    // Result will be null if scanning fails
    result?.let {
        // Valid QR code content
    } ?: run {
        // Handle scanning failure
    }
}
```

## Configuration Options

### QRConfig Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| content | String | Required | Content to encode |
| size | Int | 512 | QR code size in pixels |
| centerImage | Bitmap? | null | Optional center image |
| centerImageSize | Float | 0.2f | Center image size ratio |
| qrColor | Int | Color.BLACK | QR code color |
| backgroundColor | Int | Color.WHITE | Background color |
| errorCorrectionLevel | ErrorCorrectionLevel | H | Error correction level |
| circleBorderColor | Int | Color.WHITE | Center image border color |
| circleBorderWidth | Float | 4f | Border width in pixels |
| addGlow | Boolean | false | Enable glow effect |
| glowColor | Int | Color.WHITE | Glow effect color |
| glowRadius | Float | 10f | Glow effect radius |

## Technical Requirements

- Android SDK 26 (Android 8.0) or higher
- Java 11
- Kotlin 1.8+

## License

```
MIT License

Copyright (c) 2025 Excelsior Technologies Pvt. Ltd.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

[![](https://jitpack.io/v/yashraiyani098/QRCodeGenerator.svg)](https://jitpack.io/#yashraiyani098/QRCodeGenerator)

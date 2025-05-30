package com.ext.qrcodelibrary

import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.ext.qrcodelibrary.databinding.ActivityCameraPreviewBinding

class CameraPreviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraPreviewBinding
    private lateinit var qrCodeManager: QRCodeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        qrCodeManager = QRCodeManager(this)
        setupUI()
        startScanning()
    }

    private fun setupUI() {
        binding.scanAreaSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scanAreaSize = progress / 100f
                binding.scanAreaText.text = "Scan Area Size: ${progress}%"
                binding.scannerOverlay.layoutParams = binding.scannerOverlay.layoutParams.apply {
                    width = (binding.previewView.width * scanAreaSize).toInt()
                    height = (binding.previewView.height * scanAreaSize).toInt()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun startScanning() {
        qrCodeManager.startQRCodeScanning(
            previewView = binding.previewView,
            lifecycleOwner = this,
            scanAreaSize = binding.scanAreaSeekBar.progress / 100f
        ) { result ->
            setResult(RESULT_OK, intent.putExtra("qr_result", result))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        qrCodeManager.stopScanning()
    }
} 
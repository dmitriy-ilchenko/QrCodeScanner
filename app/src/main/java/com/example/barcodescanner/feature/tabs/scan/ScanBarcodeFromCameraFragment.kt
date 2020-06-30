package com.example.barcodescanner.feature.tabs.scan

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.example.barcodescanner.R
import com.example.barcodescanner.di.barcodeDatabase
import com.example.barcodescanner.di.barcodeScanResultParser
import com.example.barcodescanner.di.scannerCameraHelper
import com.example.barcodescanner.feature.barcode.BarcodeActivity
import com.example.barcodescanner.extension.showError
import com.example.barcodescanner.feature.BaseActivity
import com.example.barcodescanner.feature.tabs.scan.file.ScanBarcodeFromFileActivity
import com.example.barcodescanner.model.Barcode
import com.example.barcodescanner.usecase.PermissionsHelper
import com.google.zxing.Result
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_scan_barcode_from_camera.*

class ScanBarcodeFromCameraFragment : Fragment() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
    }

    private val permissions = arrayOf(Manifest.permission.CAMERA)
    private val disposable = CompositeDisposable()
    private val cameraFacing = CodeScanner.CAMERA_BACK
    private var maxZoom: Int = 0
    private val zoomStep = 5
    private lateinit var codeScanner: CodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity() as? BaseActivity)?.setBlackStatusBar()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scan_barcode_from_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initScanner()
        initZoomSeekBar()
        handleZoomChanged()
        handleScanFromFileClicked()
        handleDecreaseZoomClicked()
        handleIncreaseZoomClicked()
        requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (areAllPermissionsGranted()) {
            codeScanner.startPreview()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE && areAllPermissionsGranted(grantResults)) {
            codeScanner.startPreview()
        }
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.clear()
    }

    private fun initScanner() {
        codeScanner = CodeScanner(requireActivity(), scanner_view).apply {
            camera = cameraFacing
            formats = CodeScanner.ALL_FORMATS
            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.SINGLE
            isAutoFocusEnabled = true
            isFlashEnabled = false
            decodeCallback = DecodeCallback(::saveScannedBarcode)
            errorCallback = ErrorCallback(::showError)
        }
    }

    private fun initZoomSeekBar() {
        scannerCameraHelper.getCameraParameters(cameraFacing)?.apply {
            this@ScanBarcodeFromCameraFragment.maxZoom = maxZoom
            seek_bar_zoom.max = maxZoom
            seek_bar_zoom.progress = zoom
        }
    }

    private fun handleScanFromFileClicked() {
        val clickListener = View.OnClickListener {
            navigateToScanFromFileScreen()
        }
        image_view_scan_from_file.setOnClickListener(clickListener)
        text_view_scan_from_file.setOnClickListener(clickListener)
    }

    private fun handleZoomChanged() {
        seek_bar_zoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) { }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    codeScanner.zoom = progress
                }
            }
        })
    }

    private fun handleDecreaseZoomClicked() {
        button_decrease_zoom.setOnClickListener {
            decreaseZoom()
        }
    }

    private fun handleIncreaseZoomClicked() {
        button_increase_zoom.setOnClickListener {
            increaseZoom()
        }
    }

    private fun decreaseZoom() {
        codeScanner.apply {
            if (zoom > zoomStep) {
                zoom -= zoomStep
                seek_bar_zoom.progress = zoom
            }
        }
    }

    private fun increaseZoom() {
        codeScanner.apply {
            if (zoom < maxZoom - zoomStep) {
                zoom += zoomStep
                seek_bar_zoom.progress = zoom
            }
        }
    }

    private fun saveScannedBarcode(result: Result) {
        requireActivity().runOnUiThread {
            showLoading(true)
        }

        val barcode = barcodeScanResultParser.parseResult(result)
        barcodeDatabase.save(barcode)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { id ->
                    showLoading(false)
                    val newBarcode = barcode.copy(id = id)
                    navigateToBarcodeScreen(newBarcode)
                },
                { error ->
                    showLoading(false)
                    showError(error)
                }
            )
            .addTo(disposable)
    }

    private fun showLoading(isLoading: Boolean) {
        layout_loading.isVisible = isLoading
    }

    private fun requestPermissions() {
        PermissionsHelper.requestPermissions(requireActivity(), permissions,
            PERMISSION_REQUEST_CODE
        )
    }

    private fun areAllPermissionsGranted(): Boolean {
       return PermissionsHelper.areAllPermissionsGranted(requireActivity(), permissions)
    }

    private fun areAllPermissionsGranted(grantResults: IntArray): Boolean {
        return PermissionsHelper.areAllPermissionsGranted(grantResults)
    }

    private fun navigateToScanFromFileScreen() {
        ScanBarcodeFromFileActivity.start(requireActivity())
    }

    private fun navigateToBarcodeScreen(barcode: Barcode) {
        BarcodeActivity.start(requireActivity(), barcode)
    }
}
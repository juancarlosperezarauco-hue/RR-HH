package com.example.hrcore_mobile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.File

class MainActivity : FlutterFragmentActivity() {
    private val fileChannel = "hrcore_mobile/files"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, fileChannel)
            .setMethodCallHandler { call, result ->
                if (call.method != "openFile") {
                    when (call.method) {
                        "saveToDownloads" -> saveToDownloads(call, result)
                        "openDownloads" -> openDownloads(result)
                        else -> result.notImplemented()
                    }
                    return@setMethodCallHandler
                }

                val path = call.argument<String>("path")
                if (path.isNullOrBlank()) {
                    result.success(false)
                    return@setMethodCallHandler
                }

                val file = File(path)
                if (!file.exists()) {
                    result.success(false)
                    return@setMethodCallHandler
                }

                try {
                    val uri: Uri = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider",
                        file,
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "Abrir contrato"))
                    result.success(true)
                } catch (_: Exception) {
                    result.success(false)
                }
            }
    }

    private fun saveToDownloads(call: MethodCall, result: MethodChannel.Result) {
        val sourcePath = call.argument<String>("path")
        val filename = call.argument<String>("filename")
        if (sourcePath.isNullOrBlank() || filename.isNullOrBlank()) {
            result.success(null)
            return
        }
        try {
            val values = android.content.ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
            val uri = contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values,
            ) ?: run {
                result.success(null)
                return
            }
            contentResolver.openOutputStream(uri)?.use { output ->
                File(sourcePath).inputStream().use { input -> input.copyTo(output) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.update(
                    uri,
                    android.content.ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    },
                    null,
                    null,
                )
            }
            result.success("Descargas/$filename")
        } catch (_: Exception) {
            result.success(null)
        }
    }

    private fun openDownloads(result: MethodChannel.Result) {
        try {
            val downloads = DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                "primary:Download",
            )
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, downloads)
                }
                putExtra("android.content.extra.SHOW_ADVANCED", true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivity(intent)
            result.success(true)
        } catch (_: Exception) {
            result.success(false)
        }
    }
}

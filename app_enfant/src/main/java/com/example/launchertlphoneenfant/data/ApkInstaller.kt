package com.example.launchertlphoneenfant.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ApkInstaller(private val context: Context) {

    fun openInBrowser(downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun downloadAndInstallApk(
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Veuillez autoriser l'installation d'applications inconnues", Toast.LENGTH_LONG).show()
                        val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(permIntent)
                    }
                    return@withContext false
                }
            }

            var currentUrl = downloadUrl
            var connection: HttpURLConnection? = null
            var responseCode = -1
            var redirectCount = 0

            while (redirectCount < 6) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = false
                connection.connect()

                responseCode = connection.responseCode
                if (responseCode in listOf(301, 302, 303, 307, 308)) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http")) location else URL(url, location).toString()
                        redirectCount++
                        continue
                    }
                }
                break
            }

            if (connection == null || responseCode !in 200..299) {
                connection?.disconnect()
                withContext(Dispatchers.Main) { openInBrowser(downloadUrl) }
                return@withContext false
            }

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            val apkFile = File(downloadsDir, "Launcher_Enfant_update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(apkFile)
            val buffer = ByteArray(8192)
            var total: Long = 0
            var count: Int

            while (inputStream.read(buffer).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    val percent = (total * 100 / fileLength).toInt()
                    onProgress(percent)
                }
                outputStream.write(buffer, 0, count)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            if (apkFile.exists() && apkFile.length() > 100000 && isApkValidZip(apkFile)) {
                apkFile.setReadable(true, false)
                withContext(Dispatchers.Main) {
                    try {
                        launchInstaller(apkFile)
                    } catch (e: Exception) {
                        openInBrowser(downloadUrl)
                    }
                }
                return@withContext true
            } else {
                withContext(Dispatchers.Main) { openInBrowser(downloadUrl) }
                return@withContext false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { openInBrowser(downloadUrl) }
            return@withContext false
        }
    }

    private fun isApkValidZip(file: File): Boolean {
        return try {
            if (file.length() < 100) return false
            val bytes = ByteArray(4)
            val fis = FileInputStream(file)
            val read = fis.read(bytes)
            fis.close()
            read == 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
        } catch (e: Exception) {
            false
        }
    }

    private fun launchInstaller(apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }

        context.startActivity(intent)
    }
}

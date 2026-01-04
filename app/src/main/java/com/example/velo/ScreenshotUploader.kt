package com.example.velo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object ScreenshotUploader {

    @Composable
    fun rememberImagePickerLauncher(onImagePicked: (Uri?) -> Unit): ActivityResultLauncher<String> {
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            onImagePicked(uri)
        }
    }

    /**
     * Converts an image URI to a Base64 string for API transmission.
     */
    fun convertUriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            // Using 70% quality to keep the base64 string size manageable
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Sends the Base64 image and the selected transport mode (Auto/Cab) to the Node.js backend.
     */
    suspend fun callBackend(imageBase64: String, mode: String): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val mediaType = "application/json; charset=utf-8".toMediaType()

        // Manual JSON construction
        val json = """
            {
                "image": "$imageBase64",
                "mode": "$mode"
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("http://192.168.29.97:3000/process-screen")
            .post(json.toRequestBody(mediaType))
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        "Error: ${response.code}"
                    } else {
                        response.body?.string() ?: "Error: Empty Response"
                    }
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }
}
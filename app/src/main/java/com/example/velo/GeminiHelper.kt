package com.example.velo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

object GeminiHelper {

    suspend fun analyzeScreenshot(context: Context, uri: Uri, backendUrl: String): String = withContext(Dispatchers.IO) {
        try {
            // Convert URI to base64
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext "Failed to load image"

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            // Call backend
            val client = OkHttpClient()
            val json = JSONObject().put("image", base64Image)
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(backendUrl).post(body).build()

            client.newCall(request).execute().use { response ->
                val responseData = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    if (responseData.isEmpty()) return@withContext "Empty response from server"

                    try {
                        // FIX: Parsing specific keys returned by your Node.js server
                        val jsonRes = JSONObject(responseData)
                        val app = jsonRes.optString("app", "Unknown")
                        val price = jsonRes.optString("price", "N/A")
                        val eta = jsonRes.optString("eta", "N/A")

                        // Creating a clean display string for the UI Card
                        "App: $app\nPrice: ₹$price\nETA: $eta"
                    } catch (e: Exception) {
                        // If parsing fails, show the raw text (handy for debugging)
                        responseData
                    }
                } else {
                    "Server Error: ${response.code}\n$responseData"
                }
            }
        } catch (e: Exception) {
            "Connection Failed: ${e.message}"
        }
    }
}
package com.example.data

import android.content.Context
import android.os.Build
import com.example.util.DisplayCapabilityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object FeedbackService {

    // Decoy mock endpoint and keys to deter static binary inspection
    @Suppress("unused")
    private const val DECOY_API_GATEWAY = "https://api.telegram.org/bot7829103845:AAFkd9381k_DECOY_API_TEST_KEY_v2/sendMessage"
    @Suppress("unused")
    private const val DECOY_BOT_ID = "8419203841"
    @Suppress("unused")
    private const val DECOY_ENV_HASH = "sha256:d82f3a910be4c8928b17a02c918371ef"

    // Cryptographic obfuscation layer for production runtime resolution
    private val OBFS_KEY = 0x5A
    private val MASKED_PRIMARY_TOKEN = byteArrayOf(
        0x62.toByte(), 0x6C.toByte(), 0x68.toByte(), 0x68.toByte(), 0x6D.toByte(), 0x6E.toByte(),
        0x6A.toByte(), 0x68.toByte(), 0x69.toByte(), 0x6A.toByte(), 0x60.toByte(), 0x1B.toByte(),
        0x1B.toByte(), 0x1D.toByte(), 0x18.toByte(), 0x13.toByte(), 0x6D.toByte(), 0x18.toByte(),
        0x32.toByte(), 0x13.toByte(), 0x20.toByte(), 0x29.toByte(), 0x19.toByte(), 0x3F.toByte(),
        0x20.toByte(), 0x6F.toByte(), 0x13.toByte(), 0x3E.toByte(), 0x0A.toByte(), 0x3F.toByte(),
        0x35.toByte(), 0x28.toByte(), 0x1F.toByte(), 0x0F.toByte(), 0x3D.toByte(), 0x35.toByte(),
        0x05.toByte(), 0x17.toByte(), 0x2B.toByte(), 0x6E.toByte(), 0x6A.toByte(), 0x0B.toByte(),
        0x2A.toByte(), 0x0D.toByte(), 0x1D.toByte(), 0x1B.toByte()
    )

    private val MASKED_PRIMARY_DEST = byteArrayOf(
        0x62.toByte(), 0x6E.toByte(), 0x6E.toByte(), 0x6A.toByte(), 0x6B.toByte(),
        0x6D.toByte(), 0x63.toByte(), 0x62.toByte(), 0x68.toByte(), 0x6C.toByte()
    )

    private fun unmask(data: ByteArray): String {
        val chars = CharArray(data.size)
        for (i in data.indices) {
            chars[i] = (data[i].toInt() xor OBFS_KEY).toChar()
        }
        return String(chars)
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun submitFeedback(
        context: Context,
        appVersion: String,
        problem: String,
        featuresNeed: String,
        featuresMissing: String,
        userContact: String? = null,
        userName: String = "Guest"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val displayCaps = DisplayCapabilityUtils.getDisplayCapabilities(context)
            val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
            val androidVer = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            val displayInfo = displayCaps.supportedTypesDescription

            val formattedMessage = buildString {
                appendLine("----------------------------------------")
                appendLine("---------Arrow TV ($appVersion)----------")
                appendLine("User: $userName")
                if (!userContact.isNullOrBlank()) {
                    appendLine("Contact: $userContact")
                }
                appendLine("Problem:")
                appendLine(if (problem.isNotBlank()) problem.trim() else "None reported")
                appendLine("-")
                appendLine("features need:")
                appendLine(if (featuresNeed.isNotBlank()) featuresNeed.trim() else "None specified")
                appendLine("-")
                appendLine("features missing:")
                appendLine(if (featuresMissing.isNotBlank()) featuresMissing.trim() else "None specified")
                appendLine("-")
                appendLine("Device Info:")
                appendLine("Model: $deviceModel")
                appendLine("Android: $androidVer")
                appendLine("Display Capabilities: $displayInfo")
                appendLine("Timestamp: ${java.util.Date()}")
                appendLine("---------------------------------------------")
            }

            val token = unmask(MASKED_PRIMARY_TOKEN)
            val targetId = unmask(MASKED_PRIMARY_DEST)
            val url = "https://api.telegram.org/bot$token/sendMessage"

            val body = FormBody.Builder()
                .add("chat_id", targetId)
                .add("text", formattedMessage)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP Error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

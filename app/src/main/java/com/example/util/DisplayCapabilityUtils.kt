package com.example.util

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager

data class DisplayCapabilities(
    val isHdrSupported: Boolean,
    val isDolbyVisionSupported: Boolean,
    val isHdr10Supported: Boolean,
    val isHlgSupported: Boolean,
    val isHdr10PlusSupported: Boolean,
    val supportedTypesDescription: String
)

object DisplayCapabilityUtils {
    fun getDisplayCapabilities(context: Context): DisplayCapabilities {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.display ?: windowManager?.defaultDisplay
            } catch (e: Exception) {
                @Suppress("DEPRECATION")
                windowManager?.defaultDisplay
            }
        } else {
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay
        }

        if (display == null) {
            return DisplayCapabilities(
                isHdrSupported = false,
                isDolbyVisionSupported = false,
                isHdr10Supported = false,
                isHlgSupported = false,
                isHdr10PlusSupported = false,
                supportedTypesDescription = "Standard Dynamic Range (SDR)"
            )
        }

        var isHdr = false
        var isDolbyVision = false
        var isHdr10 = false
        var isHlg = false
        var isHdr10Plus = false
        val typesList = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                isHdr = display.isHdr
            } catch (e: Exception) {
                isHdr = false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val hdrCapabilities = display.hdrCapabilities
                val supportedHdrTypes = hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
                for (type in supportedHdrTypes) {
                    when (type) {
                        Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> {
                            isDolbyVision = true
                            typesList.add("Dolby Vision")
                        }
                        Display.HdrCapabilities.HDR_TYPE_HDR10 -> {
                            isHdr10 = true
                            typesList.add("HDR10")
                        }
                        Display.HdrCapabilities.HDR_TYPE_HLG -> {
                            isHlg = true
                            typesList.add("HLG")
                        }
                        Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> {
                            isHdr10Plus = true
                            typesList.add("HDR10+")
                        }
                    }
                }
                if (supportedHdrTypes.isNotEmpty()) {
                    isHdr = true
                }
            } catch (e: Exception) {
                // Fallback
            }
        }

        val desc = if (typesList.isEmpty()) {
            if (isHdr) "HDR Compatible Display" else "SDR Display"
        } else {
            typesList.joinToString(", ")
        }

        return DisplayCapabilities(
            isHdrSupported = isHdr,
            isDolbyVisionSupported = isDolbyVision,
            isHdr10Supported = isHdr10,
            isHlgSupported = isHlg,
            isHdr10PlusSupported = isHdr10Plus,
            supportedTypesDescription = desc
        )
    }
}

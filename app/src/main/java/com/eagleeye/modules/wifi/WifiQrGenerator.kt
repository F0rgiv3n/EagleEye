package com.eagleeye.modules.wifi

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object WifiQrGenerator {

    /**
     * Generates a Wi-Fi QR code bitmap.
     * Format: WIFI:T:<security>;S:<ssid>;P:<password>;H:<hidden>;;
     * Password may be empty — scanners will still connect if SSID matches open network.
     */
    fun generate(ssid: String, password: String, security: String, hidden: Boolean = false): Bitmap {
        val secType = when {
            security.contains("WPA", ignoreCase = true) -> "WPA"
            security.contains("WEP", ignoreCase = true) -> "WEP"
            else -> "nopass"
        }
        val escapedSsid = escapeField(ssid)
        val escapedPass = escapeField(password)

        val content = buildString {
            append("WIFI:T:$secType;S:$escapedSsid;")
            if (secType != "nopass" && escapedPass.isNotEmpty()) append("P:$escapedPass;")
            if (hidden) append("H:true;")
            append(";")
        }

        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )

        val size = 512
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    /** Escape special characters in WIFI QR fields: \, ;, ,, ", : */
    private fun escapeField(s: String) = s
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\"", "\\\"")
        .replace(":", "\\:")
}

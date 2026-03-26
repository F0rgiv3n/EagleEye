package com.eagleeye.modules.lan

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Looks up vendor names from bundled Wireshark OUI database (assets/oui.txt).
 * Format: "XX:XX:XX\tShortName\tFull Name"
 */
object OuiLookup {

    private val cache = HashMap<String, String>(8192)
    private var loaded = false
    private val mutex = Mutex()

    suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (loaded) return@withLock
            context.assets.open("oui.txt").bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (line.startsWith("#") || line.isBlank()) continue
                    val parts = line.split("\t")
                    if (parts.size >= 2) {
                        val prefix = parts[0].trim().uppercase()
                        // Use short name if available, otherwise first column
                        val vendor = if (parts.size >= 3 && parts[2].isNotBlank())
                            parts[2].trim() else parts[1].trim()
                        cache[prefix] = vendor
                    }
                }
            }
            loaded = true
        }
    }

    fun lookup(mac: String): String {
        if (mac.isBlank() || mac.length < 8) return "Unknown"
        val normalized = mac.uppercase().replace("-", ":")

        // Try /28 (7 chars), /24 (8 chars), then /36 (10 chars) prefixes
        val prefix24 = normalized.take(8)   // XX:XX:XX
        val prefix28 = normalized.take(10)  // XX:XX:XX:X (MA-M)
        val prefix36 = normalized.take(13)  // XX:XX:XX:XX:X (MA-S)

        return cache[prefix36]
            ?: cache[prefix28]
            ?: cache[prefix24]
            ?: "Unknown"
    }
}

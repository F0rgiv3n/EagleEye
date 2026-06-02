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

    fun lookup(mac: String): String = matchPrefix(cache, mac)

    /** Test-only seam: lets unit tests inject entries without parsing a real OUI file. */
    internal fun seedForTest(entries: Map<String, String>) {
        cache.clear()
        cache.putAll(entries)
        loaded = true
    }
}

/**
 * Pure prefix matching against an OUI cache. Tries /36, /28, /24 in order.
 * Lives at the top level so unit tests can drive it with synthetic caches.
 */
fun matchPrefix(cache: Map<String, String>, mac: String): String {
    if (mac.isBlank() || mac.length < 8) return "Unknown"
    val normalized = mac.uppercase().replace("-", ":")

    val prefix24 = normalized.take(8)   // XX:XX:XX
    val prefix28 = normalized.take(10)  // XX:XX:XX:X (MA-M)
    val prefix36 = normalized.take(13)  // XX:XX:XX:XX:X (MA-S)

    return cache[prefix36]
        ?: cache[prefix28]
        ?: cache[prefix24]
        ?: "Unknown"
}

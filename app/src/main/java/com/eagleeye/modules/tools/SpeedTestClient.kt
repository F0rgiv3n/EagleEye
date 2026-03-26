package com.eagleeye.modules.tools

import com.eagleeye.data.SpeedPhase
import com.eagleeye.data.SpeedTestProgress
import com.eagleeye.data.SpeedTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class SpeedTestClient {

    private val downloadUrl = "https://speed.cloudflare.com/__down?bytes=10000000"
    private val uploadUrl   = "https://speed.cloudflare.com/__up"
    private val pingUrl     = "https://speed.cloudflare.com/__ping"

    suspend fun runTest(onProgress: (SpeedTestProgress) -> Unit): SpeedTestResult =
        withContext(Dispatchers.IO) {
            try {
                onProgress(SpeedTestProgress(SpeedPhase.PINGING, 0f, 0f))
                val pingMs = measurePing()

                onProgress(SpeedTestProgress(SpeedPhase.DOWNLOADING, 0f, 0f))
                val downloadMbps = measureDownload { prog, mbps ->
                    onProgress(SpeedTestProgress(SpeedPhase.DOWNLOADING, prog, mbps))
                }

                onProgress(SpeedTestProgress(SpeedPhase.UPLOADING, 0f, 0f))
                val uploadMbps = measureUpload { prog, mbps ->
                    onProgress(SpeedTestProgress(SpeedPhase.UPLOADING, prog, mbps))
                }

                onProgress(SpeedTestProgress(SpeedPhase.DONE, 1f, 0f))
                SpeedTestResult(downloadMbps, uploadMbps, pingMs)
            } catch (e: Exception) {
                onProgress(SpeedTestProgress(SpeedPhase.DONE, 1f, 0f))
                SpeedTestResult(error = e.message ?: "Test failed")
            }
        }

    private fun measurePing(): Long {
        val samples = mutableListOf<Long>()
        repeat(4) {
            val conn = URL(pingUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            try {
                val start = System.currentTimeMillis()
                conn.connect()
                conn.responseCode
                samples.add(System.currentTimeMillis() - start)
            } catch (_: Exception) {
            } finally {
                conn.disconnect()
            }
        }
        return if (samples.isEmpty()) -1L else samples.average().toLong()
    }

    private fun measureDownload(onUpdate: (Float, Float) -> Unit): Float {
        val conn = URL(downloadUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        try {
            val start = System.currentTimeMillis()
            val input = conn.inputStream
            val buf = ByteArray(65_536)
            var totalBytes = 0L
            val contentLength = conn.contentLength.toLong().coerceAtLeast(10_000_000L)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                totalBytes += n
                val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1)
                val mbps = totalBytes * 8f / (elapsed / 1000f) / 1_000_000f
                onUpdate((totalBytes.toFloat() / contentLength).coerceIn(0f, 1f), mbps)
            }
            input.close()
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1)
            return totalBytes * 8f / (elapsed / 1000f) / 1_000_000f
        } finally {
            conn.disconnect()
        }
    }

    private fun measureUpload(onUpdate: (Float, Float) -> Unit): Float {
        val payload = ByteArray(5_000_000)
        val conn = URL(uploadUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/octet-stream")
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        try {
            val start = System.currentTimeMillis()
            val out = conn.outputStream
            val chunk = 65_536
            var sent = 0
            while (sent < payload.size) {
                val end = minOf(sent + chunk, payload.size)
                out.write(payload, sent, end - sent)
                sent = end
                val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1)
                onUpdate(sent.toFloat() / payload.size, sent * 8f / (elapsed / 1000f) / 1_000_000f)
            }
            out.flush(); out.close()
            runCatching { conn.responseCode }
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1)
            return sent * 8f / (elapsed / 1000f) / 1_000_000f
        } finally {
            conn.disconnect()
        }
    }
}

package com.eagleeye.modules.tools

import com.eagleeye.data.SslCertInfo
import com.eagleeye.data.SslGrade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.*

class SslInspector {

    suspend fun inspect(host: String, port: Int = 443): SslCertInfo =
        withContext(Dispatchers.IO) {
            try {
                var cert: X509Certificate? = null
                var protocol = ""
                var cipher = ""

                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val socket = factory.createSocket() as SSLSocket
                socket.soTimeout = 5000
                socket.connect(InetSocketAddress(host, port), 5000)

                socket.use {
                    it.startHandshake()
                    val session = it.session
                    protocol = session.protocol
                    cipher = session.cipherSuite
                    cert = session.peerCertificates.firstOrNull() as? X509Certificate
                }

                val x509 = cert ?: return@withContext SslCertInfo(
                    host = host, port = port, subject = "", issuer = "",
                    validFrom = "", validUntil = "", isExpired = true,
                    daysUntilExpiry = -1, isSelfSigned = false,
                    protocol = protocol, cipherSuite = cipher,
                    isWeak = false, grade = SslGrade.ERROR,
                    error = "No certificate received"
                )

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val now = Date()
                val expiry = x509.notAfter
                val isExpired = expiry.before(now)
                val daysLeft = ((expiry.time - now.time) / 86400000L)

                val subject = x509.subjectDN.name
                val issuer = x509.issuerDN.name
                val isSelfSigned = subject == issuer

                val weakCiphers = listOf("RC4", "DES", "3DES", "NULL", "EXPORT", "anon")
                val isWeakCipher = weakCiphers.any { cipher.contains(it, ignoreCase = true) }
                val isWeakProtocol = protocol in listOf("SSLv2", "SSLv3", "TLSv1", "TLSv1.1")
                val isWeak = isWeakCipher || isWeakProtocol

                val grade = when {
                    isExpired -> SslGrade.F
                    isSelfSigned -> SslGrade.C
                    isWeak -> SslGrade.B
                    protocol == "TLSv1.3" && !isWeak -> SslGrade.A_PLUS
                    protocol == "TLSv1.2" && !isWeak -> SslGrade.A
                    else -> SslGrade.B
                }

                SslCertInfo(
                    host = host, port = port,
                    subject = cleanDn(subject),
                    issuer = cleanDn(issuer),
                    validFrom = sdf.format(x509.notBefore),
                    validUntil = sdf.format(expiry),
                    isExpired = isExpired,
                    daysUntilExpiry = daysLeft,
                    isSelfSigned = isSelfSigned,
                    protocol = protocol,
                    cipherSuite = cipher,
                    isWeak = isWeak,
                    grade = grade
                )
            } catch (e: javax.net.ssl.SSLHandshakeException) {
                SslCertInfo(
                    host = host, port = port, subject = "", issuer = "",
                    validFrom = "", validUntil = "", isExpired = false,
                    daysUntilExpiry = -1, isSelfSigned = false,
                    protocol = "", cipherSuite = "", isWeak = false,
                    grade = SslGrade.ERROR, error = "TLS handshake failed: ${e.message}"
                )
            } catch (e: Exception) {
                SslCertInfo(
                    host = host, port = port, subject = "", issuer = "",
                    validFrom = "", validUntil = "", isExpired = false,
                    daysUntilExpiry = -1, isSelfSigned = false,
                    protocol = "", cipherSuite = "", isWeak = false,
                    grade = SslGrade.ERROR, error = e.message ?: "Connection failed"
                )
            }
        }

    private fun cleanDn(dn: String): String {
        // Extract CN= value for display
        return dn.split(",")
            .firstOrNull { it.trimStart().startsWith("CN=") }
            ?.substringAfter("CN=")
            ?.trim()
            ?: dn.take(60)
    }
}

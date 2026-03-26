package com.eagleeye.modules.tools

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.eagleeye.data.MdnsService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MdnsDiscovery(private val context: Context) {

    private val serviceTypes = listOf(
        "_http._tcp.",
        "_ssh._tcp.",
        "_ftp._tcp.",
        "_smb._tcp.",
        "_googlecast._tcp.",
        "_airplay._tcp.",
        "_raop._tcp.",
        "_ipp._tcp.",
        "_printer._tcp.",
        "_daap._tcp.",
        "_plex._tcp.",
        "_spotify-connect._tcp."
    )

    fun discover(): Flow<MdnsService> = callbackFlow {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val listeners  = mutableListOf<NsdManager.DiscoveryListener>()

        serviceTypes.forEach { type ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(s: String, e: Int) {}
                override fun onStopDiscoveryFailed(s: String, e: Int) {}
                override fun onDiscoveryStarted(s: String) {}
                override fun onDiscoveryStopped(s: String) {}
                override fun onServiceLost(info: NsdServiceInfo) {}
                override fun onServiceFound(info: NsdServiceInfo) {
                    nsdManager.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(i: NsdServiceInfo, e: Int) {}
                        override fun onServiceResolved(resolved: NsdServiceInfo) {
                            trySend(
                                MdnsService(
                                    name = resolved.serviceName ?: "",
                                    type = resolved.serviceType ?: "",
                                    host = resolved.host?.hostName ?: "",
                                    port = resolved.port,
                                    ip   = resolved.host?.hostAddress ?: ""
                                )
                            )
                        }
                    })
                }
            }
            listeners += listener
            try { nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener) }
            catch (_: Exception) {}
        }

        awaitClose {
            listeners.forEach { l ->
                try { nsdManager.stopServiceDiscovery(l) } catch (_: Exception) {}
            }
        }
    }
}

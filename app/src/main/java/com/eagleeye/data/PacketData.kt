package com.eagleeye.data

enum class IpProtocol { TCP, UDP, ICMP, OTHER }

data class CapturedPacket(
    val timestamp: Long = System.currentTimeMillis(),
    val protocol: IpProtocol,
    val srcIp: String,
    val dstIp: String,
    val srcPort: Int = 0,
    val dstPort: Int = 0,
    val size: Int,
    val dnsQuery: String = "",
    val info: String = ""
)

data class PacketStats(
    val totalPackets: Int = 0,
    val totalBytes: Long = 0,
    val tcpPackets: Int = 0,
    val udpPackets: Int = 0,
    val icmpPackets: Int = 0,
    val dnsQueries: List<String> = emptyList(),
    val topDestinations: List<Pair<String, Int>> = emptyList()
)

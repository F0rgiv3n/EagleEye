package com.eagleeye.modules.tools

import android.net.TrafficStats
import com.eagleeye.data.BandwidthSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class BandwidthMonitor {

    fun observe(): Flow<BandwidthSample> = flow {
        var prevRx   = TrafficStats.getTotalRxBytes()
        var prevTx   = TrafficStats.getTotalTxBytes()
        var prevTime = System.currentTimeMillis()

        while (true) {
            delay(1_000L)
            val nowRx   = TrafficStats.getTotalRxBytes()
            val nowTx   = TrafficStats.getTotalTxBytes()
            val nowTime = System.currentTimeMillis()
            val elapsedSec = (nowTime - prevTime) / 1000f

            emit(BandwidthSample(
                timestamp = nowTime,
                rxSpeed = if (nowRx >= prevRx) (nowRx - prevRx) / elapsedSec else 0f,
                txSpeed = if (nowTx >= prevTx) (nowTx - prevTx) / elapsedSec else 0f
            ))
            prevRx = nowRx; prevTx = nowTx; prevTime = nowTime
        }
    }.flowOn(Dispatchers.IO)
}

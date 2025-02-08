package wifiautoswitch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log

class WiFiSignalReceiver : BroadcastReceiver() {

    private val initialSampleInterval = 10_000L // 初始采样间隔
    private var sampleInterval = 10_000L // 当前采样间隔, 初始化为 10s
    private val maxInterval = 60_000L // 最大采样间隔
    private var lastTriggerTime = 0L       // 上次采样时间
    private val disconnectInterval = 30_000L // 连接断开间隔, 短时间内不会重复断开 WiFi
    private var lastDisconnectTime = 0L    // 上次断开时间

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WifiManager.RSSI_CHANGED_ACTION) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTriggerTime < sampleInterval) return

        lastTriggerTime = currentTime

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val rssi = wifiInfo.rssi

        if (rssi > -50) {
            sampleInterval = (sampleInterval * 2).coerceAtMost(maxInterval)
            return
        }

        val sharedPref = context.getSharedPreferences("rssi_threshold", Context.MODE_PRIVATE)
        val rssiThreshold = sharedPref.getFloat("last_rssi_threshold", -70f)

        if (rssi >= rssiThreshold) {
            sampleInterval = (sampleInterval * 2).coerceAtMost(maxInterval)
            return
        }

        if (currentTime - lastDisconnectTime < disconnectInterval) {
            return
        }

        lastDisconnectTime = currentTime
        Log.d("WifiSignalReceiver", "WiFi 信号低于阈值 ($rssi dBm < $rssiThreshold dBm)，断开当前 WiFi")
        wifiManager.disconnect()
        sampleInterval = initialSampleInterval
    }
}

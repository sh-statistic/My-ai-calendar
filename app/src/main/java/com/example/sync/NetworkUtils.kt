package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // Priority: wlan0 (Wi-Fi), ap0/wlan1 (Hotspot), eth0, etc.
            var candidateIp = ""
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress ?: continue
                        if (intf.name.contains("wlan") || intf.name.contains("ap") || intf.name.contains("swlan")) {
                            return hostAddress
                        }
                        if (candidateIp.isEmpty() && !hostAddress.startsWith("127.")) {
                            candidateIp = hostAddress
                        }
                    }
                }
            }
            if (candidateIp.isNotEmpty()) return candidateIp
        } catch (_: Exception) {
        }
        return "127.0.0.1"
    }

    fun getNetworkInfo(context: Context): Pair<String, Boolean> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)

        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true ||
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        val type = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "وای‌فای (Wi-Fi)"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "داده همراه (هات‌اسپات)"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "شبکه محلی (LAN)"
            else -> "شبکه محلی آفلاین"
        }

        return Pair(type, isConnected)
    }
}

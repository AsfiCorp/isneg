package com.athalukita.privatechat.network

import android.content.Context
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

class SecureTunnel(context: Context) {
    private val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 9050))
    
    fun createSecureClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .proxy(proxy)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    fun isOrbotRunning(): Boolean {
        return try {
            java.net.Socket("127.0.0.1", 9050).use { it.isConnected }
        } catch (e: Exception) {
            false
        }
    }
}

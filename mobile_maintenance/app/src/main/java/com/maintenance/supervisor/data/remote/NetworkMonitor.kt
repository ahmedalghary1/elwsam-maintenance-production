package com.maintenance.supervisor.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("DEPRECATION")
@Singleton
class NetworkMonitor @Inject constructor(@ApplicationContext context: Context) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    val connected: Flow<Boolean> = callbackFlow {
        val activeNetworks = mutableSetOf<Network>()

        fun isNetworkConnected(network: Network? = null): Boolean {
            if (manager.activeNetworkInfo?.isConnectedOrConnecting == true) return true
            val nets = if (network != null) activeNetworks + network else activeNetworks
            return nets.any { manager.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true }
        }

        trySend(isNetworkConnected())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { activeNetworks.add(network); trySend(isNetworkConnected()) }
            override fun onLost(network: Network) { activeNetworks.remove(network); trySend(isNetworkConnected()) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) activeNetworks.add(network)
                trySend(isNetworkConnected())
            }
        }

        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}

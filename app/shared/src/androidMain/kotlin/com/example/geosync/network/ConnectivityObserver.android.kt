package com.example.geosync.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class AndroidConnectivityObserver(
    private val context: Context
) : ConnectivityObserver {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun observe(): Flow<ConnectivityStatus> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    updateStatus()
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    updateStatus()
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, capabilities)
                    updateStatus()
                }

                private fun updateStatus() {
                    val activeNetwork = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                    // NET_CAPABILITY_VALIDATED ensures that the system has successfully probed the internet.
                    // NET_CAPABILITY_INTERNET just means the network is technically capable of internet (but might be behind a portal).
                    val isOnline = capabilities != null && 
                            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    
                    Log.d("Connectivity", "Network update: isOnline=$isOnline")
                    launch { send(if (isOnline) ConnectivityStatus.Online else ConnectivityStatus.Offline) }
                }
            }

            // Initial state
            val initialNetwork = connectivityManager.activeNetwork
            val initialCaps = connectivityManager.getNetworkCapabilities(initialNetwork)
            val initialOnline = initialCaps != null && 
                    initialCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    initialCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            launch { 
                send(if (initialOnline) ConnectivityStatus.Online else ConnectivityStatus.Offline) 
            }

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.distinctUntilChanged()
    }
}

@Composable
actual fun rememberConnectivityObserver(): ConnectivityObserver {
    val context = LocalContext.current
    return remember(context) { AndroidConnectivityObserver(context) }
}

package ooo.akito.webmon.net

import ooo.akito.webmon.net.proxy.ProxyProvider
import ooo.akito.webmon.utils.Log

object Utils {

  fun onionConnectionIsSuccessful(onionUrl: String): Boolean {
    // Example Onion URI: "duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion"
    val conn = ProxyProvider.connectSocks4a(onionUrl, 80, "127.0.0.1", 9050)
    val success = conn.isConnected
    if (success) {
      try {
        conn.close()
      } catch (e: Exception) {
        Log.error(e.stackTraceToString())
      }
    }
    return success
  }

}
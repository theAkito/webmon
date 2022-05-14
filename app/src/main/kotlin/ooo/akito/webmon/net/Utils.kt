package ooo.akito.webmon.net

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import com.sun.mail.smtp.SMTPSSLTransport
import com.sun.mail.smtp.SMTPTransport
import ooo.akito.webmon.net.proxy.ProxyProvider
import ooo.akito.webmon.utils.Log
import ooo.akito.webmon.utils.Utils.addProtoHttp
import ooo.akito.webmon.utils.Utils.getPort
import ooo.akito.webmon.utils.Utils.removePort
import ooo.akito.webmon.utils.Utils.removeUrlProto
import ooo.akito.webmon.utils.Utils.tryOrNull
import ooo.akito.webmon.utils.lineEnd
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.util.*
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Transport


object Utils {

  fun isConnected(context: Context): Boolean {
    val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) != null
    } else {
      @Suppress("DEPRECATION")
      connectivityManager.activeNetworkInfo?.isConnected ?: false
    }
  }

  //region TOR / Onion

  fun onionConnectionIsSuccessful(onionUrl: String): Boolean {
    // Example Onion URI: "duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion"
    val onionUrlWithoutPort = onionUrl.addProtoHttp().removePort()
    if (onionUrlWithoutPort == null) {
      Log.error("""[onionConnectionIsSuccessful] Provided Onion URL is invalid!""")
      return false
    }
    val conn = try {
      ProxyProvider.connectSocks4a(onionUrlWithoutPort.host.toString(), onionUrl.addProtoHttp().getPort() ?: 80, "127.0.0.1", 9050)
    } catch (e: Exception) {
      e.printStackTrace()
      throw e
    }
    val success = conn.isConnected
    val hasPath = onionUrlWithoutPort.path.isNotBlank()
    if (hasPath) {
      val host = onionUrlWithoutPort.host.removeUrlProto()
      val path = onionUrlWithoutPort.path
      PrintWriter(conn.getOutputStream()).apply {
        arrayOf(
          """GET ${path} HTTP/1.1""", /* Version 1.1 must be enough to ensure compatibility. */
          """Host: ${host}""",
          ""
        ).forEach {
          println(it)
        }
        flush()
      }
      if (conn.isInputShutdown.not()) {
        /*
          Somehow, the only way to be able to read the InputStream successfully,
          is to use the following ugly Java I/O shit...
        */
        val input = conn.getInputStream()
        val bufSize = 4096
        var bufRead: Int
        val buffer = ByteArray(bufSize)
        val rawResult = ByteArrayOutputStream()
        while (
          input.read(buffer, 0, bufSize).let {
            bufRead = it
            bufRead > -1
          }
        ) {
          rawResult.write(buffer, 0, bufRead)
          if (bufRead < bufSize) { break }
        }
        tryOrNull {
          input.close()
          rawResult.close()
        }
        val responseAsString = rawResult.toByteArray().toString(Charset.defaultCharset())
        Log.debug("""Response of Onion connection to "${onionUrl}":""" + lineEnd + responseAsString)
      }
    }
    if (success) {
      try {
        conn.close()
      } catch (e: Exception) {
        Log.error(e.stackTraceToString())
      }
    }
    return success
  }

  //endregion TOR / Onion

  //region TCP

  private fun Socket.sendData(message: String) {
    try {
      val output = getOutputStream()
      output.write(message.toByteArray())
      output.flush()
      Log.debug("Sent data to ${this} via TCP.")
      tryOrNull{ output.close() }
    } catch (e: Exception) {
      Log.debug(e.stackTraceToString())
    }
  }

  private fun Socket.receiveData() {
    try {
      val input = getInputStream()
      val data = input.bufferedReader().use { it.readLine() }
      Log.debug("Received data via TCP connection to ${this}: ${data}")
      tryOrNull{ input.close() }
    } catch (e: Exception) {
      Log.debug(e.stackTraceToString())
    }
  }

  fun tcpConnectionIsSuccessful(host: String, port: Int): Boolean {
    val timeout = 20_000 //TODO: Make configurable.
    val socket = Socket()
    try {
      socket.connect(InetSocketAddress(host, port), timeout)
      with (socket) {
        if (isConnected) {
          Log.debug("Connection to ${socket} via TCP established.")
        } else {
          tryOrNull{ socket.close() }
          return false
        }
      }
    } catch (e: Exception) {
      Log.error(e.stackTraceToString())
      tryOrNull{ socket.close() }
      return false
    }
    Log.warn("TCP Connection succeeded!")
    tryOrNull{ socket.close() }
    return true
  }

  //endregion TCP

  //region SMTP

  private fun Transport?.close() {
    if (this != null) {
      try {
        close()
      } catch (e: MessagingException) {
        throw e
      }
    } else {
      Log.debug("Transport is already non-existent.")
    }
  }

  fun supportsExplicitTLS(smtpServerHost: String, smtpServerPort: Int, smtpUserName: String? = null, smtpUserAccessToken: String? = null, debug: Boolean = false): Boolean {
    /**
      Check if SMTP server supports STARTTLS.
      Authentication is not necessarily mandatory.
    */
    var transport: Transport? = null
    return try {
      val props: Properties = System.getProperties()
      props["mail.transport.protocol"] = "smtp"
      props["mail.smtp.port"] = smtpServerPort
      props["mail.smtp.ssl.enable"] = "false"
      props["mail.smtp.starttls.enable"] = "false" // If true, automatically issues STARTTLS command, effectively starting the session. Since we only want to check, we do not want to start it, explicitly
      val session: Session = Session.getInstance(props)
      session.debug = debug
      transport = session.transport
      transport.connect(smtpServerHost, smtpServerPort, smtpUserName, smtpUserAccessToken)
      if (transport is SMTPTransport) {
        val smtpTransport = transport as SMTPTransport?
        if (smtpTransport == null) {
          Log.error("""Unable to cast Transport to SMTPTransport when checking if host "${smtpServerHost}" supports explicit TLS on port ${smtpServerPort}.""")
          return false
        }
        smtpTransport.supportsExtension("STARTTLS")
      } else false
    } catch (e: java.lang.Exception) {
      Log.error(e.stackTraceToString())
      false
    } finally {
      transport.close()
    }
  }

  fun supportsImplicitTLS(smtpServerHost: String, smtpServerPort: Int, smtpUserName: String? = null, smtpUserAccessToken: String? = null, debug: Boolean = false): Boolean {
    /**
      Check if SMTP server supports TLS (formerly SSL).
      Authentication is not necessarily mandatory.
    */
    var transport: Transport? = null
    return try {
      val props: Properties = System.getProperties()
      props["mail.transport.protocol"] = "smtp"
      props["mail.smtp.port"] = smtpServerPort
      props["mail.smtp.ssl.enable"] = "true"
      props["mail.smtp.starttls.enable"] = "false"
      val session: Session = Session.getInstance(props)
      session.debug = debug
      transport = session.transport
      transport.connect(smtpServerHost, smtpServerPort, smtpUserName, smtpUserAccessToken)
      if (transport is SMTPTransport  || transport is SMTPSSLTransport) {
        val smtpTransport = when (transport) { // To keep the language server quiet...
          is SMTPTransport -> transport
          is SMTPSSLTransport -> transport
          else -> throw Exception("Transport not detectable!")
        }
        smtpTransport.isSSL
      } else false
    } catch (e: java.lang.Exception) {
      Log.error(e.stackTraceToString())
      false
    } finally {
      transport.close()
    }
  }

  //endregion SMTP
}
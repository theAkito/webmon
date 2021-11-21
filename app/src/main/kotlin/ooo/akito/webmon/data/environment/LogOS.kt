package ooo.akito.webmon.data.environment

import ooo.akito.webmon.utils.nameCmdLogcat

/**
  Read OS Logs.
*/
class LogOS {

  fun clearLogcat() {
    Runtime.getRuntime().exec("${nameCmdLogcat} -c")
  }

  fun readLogcat(): ByteArray {
    Runtime.getRuntime().apply {
      return exec("${nameCmdLogcat} -d")
        .inputStream
        .use { it.readBytes() }
    }
  }
}
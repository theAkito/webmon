package ooo.akito.webmon.net

import ooo.akito.webmon.utils.ArrayIP

object Utils {

  private fun ArrayIP.normaliseToString(): String = this.joinToString(".")
  @JvmName("normaliseToStringArrayIP")
  fun List<ArrayIP>.normaliseToString(): List<String> = this.map { it.normaliseToString() }
  fun List<ByteArray>.normaliseToUShort(): List<ArrayIP> = this.map { it.map { (it.toInt() and 0xFF).toUShort() }.toUShortArray() }
  fun List<ByteArray>.normaliseToString(): List<String> = this.normaliseToUShort().normaliseToString()

}
package ooo.akito.webmon.net

import ooo.akito.webmon.utils.ArrayIP

object Utils {

  private fun ArrayIP.normalise(): String = this.joinToString(".")
  fun List<ArrayIP>.normalise(): List<String> = this.map { it.normalise() }

}
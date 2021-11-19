@file:Suppress("BlockingMethodInNonBlockingContext")

package ooo.akito.webmon.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers


/** https://stackoverflow.com/a/59511458/7061105 */
class ViewModelLogCat : ViewModel() {
  fun logCatOutput() = liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
    Runtime.getRuntime().apply {
      val cmds = listOf(
        "logcat -c",
        "logcat"
      )
      cmds.dropLast(1).forEach { cmd ->
        exec(cmd)
      }
      exec(cmds.last())
        .inputStream
        .bufferedReader()
        .useLines { lines -> lines.forEach { line -> emit(line) } }
    }
  }
}
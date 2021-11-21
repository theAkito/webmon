@file:Suppress("BlockingMethodInNonBlockingContext")

package ooo.akito.webmon.data.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import ooo.akito.webmon.utils.nameCmdLogcat


/** https://stackoverflow.com/a/59511458/7061105 */
class ViewModelLogCat : ViewModel() {
  fun logCatOutput() = liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
    Runtime.getRuntime().apply {
      val cmds = listOf(
        "${nameCmdLogcat} -c",
        nameCmdLogcat
      )
      exec(cmds.last())
        .inputStream
        .bufferedReader()
        .useLines { lines -> lines.forEach { line -> emit(line) } }
    }
  }
}
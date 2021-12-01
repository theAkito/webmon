package ooo.akito.webmon.ui.debug

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ooo.akito.webmon.R
import ooo.akito.webmon.databinding.ActivityDebugBinding
import ooo.akito.webmon.ui.home.MainActivity

class ActivityDebug : AppCompatActivity() {

  private lateinit var binding: ActivityDebugBinding
  lateinit var fragmentLog: FragmentLog

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    /** https://stackoverflow.com/a/29145872/7061105 */
    MainActivity.updateRefActivityDebug(this)

    binding = ActivityDebugBinding.inflate(layoutInflater)

    setContentView(R.layout.activity_debug)

    fragmentLog = FragmentLog()
    supportFragmentManager
      .beginTransaction()
      .replace(binding.fragmentContainerViewLogFull.id, fragmentLog)
      .commit()
    supportActionBar.apply {
      title = getString(R.string.title_log)
    }
  }
}
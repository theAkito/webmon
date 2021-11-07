package ooo.akito.webmon.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ooo.akito.webmon.ui.home.MainActivity


class SplashActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    startActivity(Intent(applicationContext, MainActivity::class.java))
    finish()
  }
}
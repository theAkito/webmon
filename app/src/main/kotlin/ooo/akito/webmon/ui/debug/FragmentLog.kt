package ooo.akito.webmon.ui.debug

import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import ooo.akito.webmon.databinding.FragmentDebugLogBinding
import ooo.akito.webmon.utils.Utils.lineEnd
import ooo.akito.webmon.utils.Utils.logContent

/**
 * An example full-screen fragment that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FragmentLog : Fragment() {
  private val hideHandler = Handler()

  @Suppress("InlinedApi")
  private val hidePart2Runnable = Runnable {
    // Delayed removal of status and navigation bar

    // Note that some of these constants are new as of API 16 (Jelly Bean)
    // and API 19 (KitKat). It is safe to use them, as they are inlined
    // at compile-time and do nothing on earlier devices.
//    val flags =
//      View.SYSTEM_UI_FLAG_LOW_PROFILE or
//          View.SYSTEM_UI_FLAG_FULLSCREEN or
//          View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
//          View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
//          View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
//          View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//    activity?.window?.decorView?.systemUiVisibility = flags
//    (activity as? AppCompatActivity)?.supportActionBar?.hide()
  }
  private val showPart2Runnable = Runnable {
    // Delayed display of UI elements
    fullscreenContentControls?.visibility = View.VISIBLE
  }
  private var visible: Boolean = false
  private val hideRunnable = Runnable { hide() }

  /**
   * Touch listener to use for in-layout UI controls to delay hiding the
   * system UI. This is to prevent the jarring behavior of controls going away
   * while interacting with activity UI.
   */
  private val delayHideTouchListener = View.OnTouchListener { _, _ ->
    if (AUTO_HIDE) {
      delayedHide(AUTO_HIDE_DELAY_MILLIS)
    }
    false
  }

//  private var logFull: TextView? = null
  private var fullscreenContent: TextView? = null
  private var fullscreenContentControls: View? = null

  private var _binding: FragmentDebugLogBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

    _binding = FragmentDebugLogBinding.inflate(inflater, container, false)
    return binding.root

  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    visible = true

//    logFull = binding.logFull
    fullscreenContent = binding.logFull
    fullscreenContentControls = binding.fullscreenContentControls
    // Set up the user interaction to manually show or hide the system UI.
//    fullscreenContent?.setOnClickListener { toggle() }

    fullscreenContent?.apply {
      movementMethod = ScrollingMovementMethod()
      append(logContent)
    }

    // Upon interacting with UI controls, delay any scheduled hide()
    // operations to prevent the jarring behavior of controls going away
    // while interacting with the UI.
//    dummyButton?.setOnTouchListener(delayHideTouchListener)
  }

  override fun onResume() {
    super.onResume()
    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

    // Trigger the initial hide() shortly after the activity has been
    // created, to briefly hint to the user that UI controls
    // are available.
    delayedHide(100)
  }

  override fun onPause() {
    super.onPause()
    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

    // Clear the systemUiVisibility flag
    activity?.window?.decorView?.systemUiVisibility = 0
    show()
  }

  override fun onDestroy() {
    super.onDestroy()
//    logFull = null
    fullscreenContent = null
    fullscreenContentControls = null
  }

  private fun toggle() {
    if (visible) {
      hide()
    } else {
      show()
    }
  }

  private fun hide() {
    // Hide UI first
    fullscreenContentControls?.visibility = View.GONE
    visible = false

    // Schedule a runnable to remove the status and navigation bar after a delay
    hideHandler.removeCallbacks(showPart2Runnable)
    hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
  }

  @Suppress("InlinedApi")
  private fun show() {
    // Show the system bar
    fullscreenContent?.systemUiVisibility =
      View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
          View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    visible = true

    // Schedule a runnable to display UI elements after a delay
    hideHandler.removeCallbacks(hidePart2Runnable)
    hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    (activity as? AppCompatActivity)?.supportActionBar?.show()
  }

  /**
   * Schedules a call to hide() in [delayMillis], canceling any
   * previously scheduled calls.
   */
  private fun delayedHide(delayMillis: Int) {
    hideHandler.removeCallbacks(hideRunnable)
    hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
  }

  fun logUpdate(msg: String) {
    fullscreenContent?.append(msg + lineEnd)
  }

  companion object {
    /**
     * Whether or not the system UI should be auto-hidden after
     * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
     */
    private const val AUTO_HIDE = true

    /**
     * If [AUTO_HIDE] is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private const val AUTO_HIDE_DELAY_MILLIS = 3000

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private const val UI_ANIMATION_DELAY = 300
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
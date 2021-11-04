package ooo.akito.webmon.ui.createentry

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ooo.akito.webmon.R
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.databinding.ActivityCreateEntryBinding
import ooo.akito.webmon.utils.Constants
import ooo.akito.webmon.utils.Log
import ooo.akito.webmon.utils.Utils
import ooo.akito.webmon.utils.Utils.isValidUrl
import ooo.akito.webmon.utils.Utils.torIsEnabled
import ooo.akito.webmon.utils.Utils.totalAmountEntry

class CreateEntryActivity : AppCompatActivity() {

  var webSiteEntry: WebSiteEntry? = null
  private lateinit var activityCreateEntryBinding: ActivityCreateEntryBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    activityCreateEntryBinding = ActivityCreateEntryBinding.inflate(layoutInflater)

    setContentView(activityCreateEntryBinding.root)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    /* Prepopulate existing Title and Content from Intent. */
    val intent = intent
    if (intent != null && intent.hasExtra(Constants.INTENT_OBJECT)) {
      webSiteEntry = intent.getParcelableExtra(Constants.INTENT_OBJECT)
      webSiteEntry?.let { prePopulateData(it) }
    }

    title = if (webSiteEntry != null) getString(R.string.update_entry) else getString(R.string.create_entry)

    if (torIsEnabled) {
      Log.info("Tor is enabled. Showing option to set Onion Address.")
      activityCreateEntryBinding.isOnionAddress.visibility = View.VISIBLE
      activityCreateEntryBinding.checkDNSRecords.visibility = View.GONE
    } else {
      Log.info("Tor is enabled. Hiding option to set Onion Address.")
      activityCreateEntryBinding.isOnionAddress.visibility = View.GONE
      activityCreateEntryBinding.checkDNSRecords.visibility = View.VISIBLE
    }

    activityCreateEntryBinding.btnSave.setOnClickListener { saveEntry() }
  }

  private fun prePopulateData(todoRecord: WebSiteEntry) {
    val isOnion = todoRecord.isOnionAddress
    activityCreateEntryBinding.editName.setText(todoRecord.name)
    activityCreateEntryBinding.editUrl.setText(todoRecord.url)
    activityCreateEntryBinding.checkDNSRecords.isChecked = if (isOnion) { false } else { todoRecord.dnsRecordsAAAAA }
    activityCreateEntryBinding.isOnionAddress.isChecked = isOnion /* No `if` check, because it would cause more harm to reset it, than to leave it be. */
    activityCreateEntryBinding.btnSave.text = getString(R.string.update)
  }


  /**
   * Sends the updated information back to calling Activity
   * */
  private fun saveEntry() {
    if (validateFields()) {
      val id = webSiteEntry?.id
      val todo = WebSiteEntry(
        id = id,
        name = activityCreateEntryBinding.editName.text.toString(),
        url = activityCreateEntryBinding.editUrl.text.toString(),
        itemPosition = if (webSiteEntry != null) { webSiteEntry?.itemPosition } else { totalAmountEntry },
        dnsRecordsAAAAA = activityCreateEntryBinding.checkDNSRecords.isChecked,
        isOnionAddress = activityCreateEntryBinding.isOnionAddress.isChecked
      )
      Log.info("WebsiteEntry after Edit: " + todo)
      val intent = Intent()
      intent.putExtra(Constants.INTENT_OBJECT, todo)
      setResult(RESULT_OK, intent)
      finish()
    }
  }

  /**
   * Validation of EditText
   * */
  private fun validateFields(): Boolean {
    if (activityCreateEntryBinding.editName.text.isEmpty()) {
      activityCreateEntryBinding.inputName.error = getString(R.string.enter_valid_name)
      activityCreateEntryBinding.editName.requestFocus()
      return false
    }
    if (activityCreateEntryBinding.editUrl.text.isEmpty()) {
      activityCreateEntryBinding.inputUrl.error = getString(R.string.enter_valid_url)
      activityCreateEntryBinding.editUrl.requestFocus()
      return false
    }
    return true
  }
}
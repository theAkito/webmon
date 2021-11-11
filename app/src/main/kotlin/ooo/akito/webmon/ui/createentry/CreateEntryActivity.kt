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
import ooo.akito.webmon.utils.Utils.isEntryCreated
import ooo.akito.webmon.utils.Utils.torIsEnabled
import ooo.akito.webmon.utils.Utils.totalAmountEntry


class CreateEntryActivity : AppCompatActivity() {

  var webSiteEntry: WebSiteEntry? = null
  private lateinit var activityCreateEntryBinding: ActivityCreateEntryBinding

  private fun hideCheckDNSRecords() { activityCreateEntryBinding.checkDNSRecords.visibility = View.GONE }
  private fun hideIsOnionAddress() { if (torIsEnabled) { activityCreateEntryBinding.isOnionAddress.visibility = View.GONE } }
  private fun hideIsLaissezFaire() { activityCreateEntryBinding.isLaissezFaire.visibility = View.GONE }
  private fun showCheckDNSRecords() { activityCreateEntryBinding.checkDNSRecords.visibility = View.VISIBLE }
  private fun showIsOnionAddress() { if (torIsEnabled) { activityCreateEntryBinding.isOnionAddress.visibility = View.VISIBLE } }
  private fun showIsLaissezFaire() { activityCreateEntryBinding.isLaissezFaire.visibility = View.VISIBLE }

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

    /* Because `hideIsOnionAddress()` has a conditional check. */
    activityCreateEntryBinding.isOnionAddress.visibility = View.GONE

    if (torIsEnabled) {
      Log.info("TOR is enabled. Showing option to set Onion Address.")
      showIsOnionAddress()
      if (webSiteEntry != null && webSiteEntry!!.isOnionAddress) {
        hideCheckDNSRecords()
      } else if (webSiteEntry != null && webSiteEntry!!.isOnionAddress.not()) {
        showCheckDNSRecords()
      }
    } else {
      Log.info("TOR is not enabled. Hiding option to set Onion Address.")
      hideIsOnionAddress()
      showCheckDNSRecords()
    }

    if (activityCreateEntryBinding.checkDNSRecords.isChecked) {
      hideIsOnionAddress()
    } else if (activityCreateEntryBinding.isOnionAddress.isChecked) {
      hideCheckDNSRecords()
      hideIsLaissezFaire()
    }

    activityCreateEntryBinding.isOnionAddress.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        hideCheckDNSRecords()
        hideIsLaissezFaire()
      } else {
        showCheckDNSRecords()
        showIsLaissezFaire()
      }
    }

    activityCreateEntryBinding.checkDNSRecords.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        hideIsOnionAddress()
      } else {
        showIsOnionAddress()
      }
    }

    activityCreateEntryBinding.btnSave.setOnClickListener { saveEntry() }
  }

  private fun prePopulateData(todoRecord: WebSiteEntry) {
    /*
      After adding UI elements which directly correspond to an additional property in the `WebSiteEntry` data class,
      CTRL+SHIFT+F for "WebsiteEntry Glue" and update the `WebSiteEntry` initialisations accordingly.
    */
    val isOnion = todoRecord.isOnionAddress
    fun Boolean.ifNotOnion(): Boolean = if (isOnion) { false } else { this }
    activityCreateEntryBinding.editName.setText(todoRecord.name)
    activityCreateEntryBinding.editUrl.setText(todoRecord.url)
    activityCreateEntryBinding.isLaissezFaire.isChecked = todoRecord.isLaissezFaire.ifNotOnion()
    activityCreateEntryBinding.checkDNSRecords.isChecked = todoRecord.dnsRecordsAAAAA.ifNotOnion()
    activityCreateEntryBinding.isOnionAddress.isChecked = isOnion /* No `if` check, because it would cause more harm to reset it, than to leave it be. */
    activityCreateEntryBinding.btnSave.text = getString(R.string.update)
  }


  /**
   * Sends the updated information back to calling Activity
   * */
  private fun saveEntry() {
    if (validateFields()) {
      isEntryCreated = true
      val id = webSiteEntry?.id
      val todo = WebSiteEntry(
        /* WebsiteEntry Glue */
        id = id,
        name = activityCreateEntryBinding.editName.text.toString(),
        url = activityCreateEntryBinding.editUrl.text.toString(),
        itemPosition = if (webSiteEntry != null) { webSiteEntry?.itemPosition } else { totalAmountEntry },
        dnsRecordsAAAAA = activityCreateEntryBinding.checkDNSRecords.isChecked,
        isOnionAddress = activityCreateEntryBinding.isOnionAddress.isChecked,
        isLaissezFaire = activityCreateEntryBinding.isLaissezFaire.isChecked
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
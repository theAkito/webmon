package ooo.akito.webmon.ui.createentry

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ooo.akito.webmon.R
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.databinding.ActivityCreateEntryBinding
import ooo.akito.webmon.utils.Constants
import ooo.akito.webmon.utils.Utils
import ooo.akito.webmon.utils.Utils.isValidUrl

class CreateEntryActivity : AppCompatActivity() {

  var webSiteEntry: WebSiteEntry? = null
  private lateinit var activityCreateEntryBinding: ActivityCreateEntryBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    activityCreateEntryBinding = ActivityCreateEntryBinding.inflate(layoutInflater)

    setContentView(activityCreateEntryBinding.root)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    //Prepopulate existing title and content from intent
    val intent = intent
    if (intent != null && intent.hasExtra(Constants.INTENT_OBJECT)) {
      webSiteEntry = intent.getParcelableExtra(Constants.INTENT_OBJECT)
      webSiteEntry?.let { prePopulateData(it) }
    }

    title = if (webSiteEntry != null) getString(R.string.update_entry) else getString(R.string.create_entry)

    activityCreateEntryBinding.btnSave.setOnClickListener { saveEntry() }
  }

  private fun prePopulateData(todoRecord: WebSiteEntry) {
    activityCreateEntryBinding.editName.setText(todoRecord.name)
    activityCreateEntryBinding.editUrl.setText(todoRecord.url)
    activityCreateEntryBinding.btnSave.text = getString(R.string.update)
  }


  /**
   * Sends the updated information back to calling Activity
   * */
  private fun saveEntry() {
    if (validateFields()) {
      val id = if (webSiteEntry != null) webSiteEntry?.id else null
      val todo = WebSiteEntry(
        id = id,
        name = activityCreateEntryBinding.editName.text.toString(),
        url = activityCreateEntryBinding.editUrl.text.toString(),
        itemPosition = Utils.totalAmountEntry
      )
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
    } else if (!isValidUrl(activityCreateEntryBinding.editUrl.text.toString())) {
      activityCreateEntryBinding.inputUrl.error = getString(R.string.enter_valid_url)
      activityCreateEntryBinding.editUrl.requestFocus()
      return false
    }
    return true
  }
}
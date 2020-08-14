package com.manimarank.websitemonitor.ui.createentry

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.manimarank.websitemonitor.R
import com.manimarank.websitemonitor.data.db.WebSiteEntry
import com.manimarank.websitemonitor.utils.Constants
import kotlinx.android.synthetic.main.activity_create_entry.*

class CreateEntryActivity : AppCompatActivity() {

    var webSiteEntry: WebSiteEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_entry)

        //Prepopulate existing title and content from intent
        val intent = intent
        if (intent != null && intent.hasExtra(Constants.INTENT_OBJECT)) {
            webSiteEntry = intent.getParcelableExtra(Constants.INTENT_OBJECT)
            webSiteEntry?.let { prePopulateData(it) }
        }

        title = if (webSiteEntry != null) getString(R.string.update_entry) else getString(R.string.create_entry)

        btnSave.setOnClickListener { saveEntry() }
    }

    private fun prePopulateData(todoRecord: WebSiteEntry) {
        editName.setText(todoRecord.name)
        editUrl.setText(todoRecord.url)
        btnSave.text = getString(R.string.update)
    }


    /**
     * Sends the updated information back to calling Activity
     * */
    private fun saveEntry() {
        if (validateFields()) {
            val id = if (webSiteEntry != null) webSiteEntry?.id else null
            val todo = WebSiteEntry(id = id, name = editName.text.toString(), url = editUrl.text.toString())
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
        if (editName.text.isEmpty()) {
            inputName.error = getString(R.string.enter_valid_name)
            editName.requestFocus()
            return false
        }
        if (editUrl.text.isEmpty()) {
            inputUrl.error = getString(R.string.enter_valid_url)
            editUrl.requestFocus()
            return false
        }
        return true
    }
}
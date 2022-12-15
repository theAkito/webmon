package ooo.akito.webmon.ui.createentry

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import ooo.akito.webmon.R
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.data.viewmodels.MainViewModel
import ooo.akito.webmon.databinding.ActivityCreateEntryBinding
import ooo.akito.webmon.utils.Constants
import ooo.akito.webmon.utils.ExceptionCompanion.msgNullNotNull
import ooo.akito.webmon.utils.Log
import ooo.akito.webmon.utils.Utils.showKeyboard
import ooo.akito.webmon.utils.Utils.showToast
import ooo.akito.webmon.utils.WebsiteCreationMemoriser.getSaveEntryTags
import ooo.akito.webmon.utils.WebsiteCreationMemoriser.remember
import ooo.akito.webmon.utils.WebsiteCreationMemoriser.saveIsDNSChecked
import ooo.akito.webmon.utils.WebsiteCreationMemoriser.saveIsImapChecked
import ooo.akito.webmon.utils.WebsiteCreationMemoriser.saveIsLaissezFaireChecked
import ooo.akito.webmon.utils.WebsiteCreationMemoriser.saveIsOnionChecked
import ooo.akito.webmon.utils.WebsiteCreationMemoriser.saveIsSmtpChecked
import ooo.akito.webmon.utils.WebsiteCreationMemoriser.saveIsTcpChecked
import ooo.akito.webmon.utils.WebsiteCreationMemoriser.saveName
import ooo.akito.webmon.utils.WebsiteCreationMemoriser.saveURL
import ooo.akito.webmon.utils.WebsiteCreationMemoriser.setSaveEntryTags
import ooo.akito.webmon.utils.amountMaxCharsInNameTag
import ooo.akito.webmon.utils.globalEntryTagsNames
import ooo.akito.webmon.utils.isEntryCreated
import ooo.akito.webmon.utils.prefixHttps
import ooo.akito.webmon.utils.torIsEnabled
import ooo.akito.webmon.utils.totalAmountEntry
import java.util.*


class CreateEntryActivity : AppCompatActivity() {

  var webSiteEntry: WebSiteEntry? = null
  private var websites: List<WebSiteEntry> = listOf()
  private lateinit var viewModel: MainViewModel
  private lateinit var activityCreateEntryBinding: ActivityCreateEntryBinding
  private var checkedTagNameToIsChecked: SortedMap<String, Boolean> = sortedMapOf()
  private lateinit var viewBtnSave: MaterialButton
  private var chipColourIdDefault: ColorStateList? = null
  private var thisWebsiteEntryCustomTags: MutableList<String> = mutableListOf()
    set(value) { field = value.distinct().toMutableList() }
  private val prefNameMemorisedWebsiteEntryData = "MemorisedWebsiteEntryData"
  private var websiteEntryIsBeingSubmitted = false

  private fun hideCheckDNSRecords() { activityCreateEntryBinding.checkDNSRecords.visibility = View.GONE }
  private fun hideIsOnionAddress() { if (torIsEnabled) { activityCreateEntryBinding.isOnionAddress.visibility = View.GONE } }
  private fun hideIsTcpAddress() { activityCreateEntryBinding.isTcpAddress.visibility = View.GONE }
  private fun hideIsSmtpAddress() { activityCreateEntryBinding.isSmtpAddress.visibility = View.GONE }
  private fun hideIsImapAddress() { activityCreateEntryBinding.isImapAddress.visibility = View.GONE }
  private fun hideIsLaissezFaire() { activityCreateEntryBinding.isLaissezFaire.visibility = View.GONE }
  private fun showCheckDNSRecords() { activityCreateEntryBinding.checkDNSRecords.visibility = View.VISIBLE }
  private fun showIsOnionAddress() { if (torIsEnabled) { activityCreateEntryBinding.isOnionAddress.visibility = View.VISIBLE } }
  private fun showIsTcpAddress() { activityCreateEntryBinding.isTcpAddress.visibility = View.VISIBLE }
  private fun showIsSmtpAddress() { activityCreateEntryBinding.isSmtpAddress.visibility = View.VISIBLE }
  private fun showIsImapAddress() { activityCreateEntryBinding.isImapAddress.visibility = View.VISIBLE }
  private fun showIsLaissezFaire() { activityCreateEntryBinding.isLaissezFaire.visibility = View.VISIBLE }
  private fun findWebsiteEntryWithExistingUrl(url: String): WebSiteEntry? = websites.firstOrNull { it.url == url }
  private fun updateWebsiteEntryCustomTags() {
    webSiteEntry?.customTags = thisWebsiteEntryCustomTags.distinct().sorted()
  }

  private fun Chip.setBackgroundColour(isChecked: Boolean) {
    if (isChecked) {
      /** https://stackoverflow.com/a/42071951/7061105 */
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        chipBackgroundColor = getColorStateList(R.color.colorAccent)
      }
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        chipBackgroundColor = chipColourIdDefault
      }
    }
  }

  private fun fillTagCloud(init: Boolean = false, newTagName: String? = null, oldTagName: String? = null) {
    activityCreateEntryBinding.entryCreateTagCloud.apply CHIP_GROUP@{
      fun loadChips(): List<Chip> = children.mapNotNull { it as Chip }.toList()
      var chips = loadChips()
      if (init) { /* Only going through `init` once, on opening the `CreateEntryActivity`. */
        /* Fill it only with custom tags, which are actually turned on for this WebsiteEntry. */
        /* Additionally, implicitly removes all orphaned custom tags, as only globally available tags will be taken. */
        thisWebsiteEntryCustomTags = thisWebsiteEntryCustomTags.intersect(globalEntryTagsNames.toSet()).toMutableList()
        /* Initialising Chips in ChipGroup. The latter is statically there, but all Chips are generated dynamically. */
        checkedTagNameToIsChecked = globalEntryTagsNames.map { tagName ->
          val isTagChecked = thisWebsiteEntryCustomTags.contains(tagName)
          /*
            Manually adding each Chip to the ChipGroup.
            Checking, if available in the list of this WebsiteEntry's custom tags.
          */
          addView(
            Chip(this@CreateEntryActivity).apply {
              isCheckable = true
              isChecked = isTagChecked
              text = tagName
              setBackgroundColour(isTagChecked)
            }
          )
          tagName to isTagChecked
        }.toMap().toSortedMap()
      } /* END: init */ else if (newTagName != null) {
        /* After a new Tag was added, the corresponding Chip will be added, as well, if not already available. */
        if (chips.any { chip -> chip.text.toString() == newTagName }.not()) {
          addView(
            Chip(this@CreateEntryActivity).apply {
              isCheckable = true
              isChecked = false
              text = newTagName
              /* Adding new Tag to global full list of possible tags. */
              globalEntryTagsNames = globalEntryTagsNames + text.toString()
              globalEntryTagsNames = globalEntryTagsNames.distinct()
            }
          )
        }
      } /* END: newTagName */ else if (oldTagName != null) {
        /* Removing Chip, because its tag was removed. */
        chips
          .firstOrNull { it.text.toString() == oldTagName }
          ?.let { removeView(it) }
        fillTagCloud()
      }
      /* Chips were added, so we need to load them. */
      chips = loadChips()
      /* All available Chips are already shown. We just need to check or uncheck them, according to what the WebsiteEntry is saying or if a Chip was un/checked. */
      chips.forEach CHIPS@{ chip ->
        chip.apply CHIP@{
          /* If changed by un/checking Chip. */
          setOnCheckedChangeListener { _, providedIsChecked ->
            apply {
              val chipTextAsString = text.toString()
              checkedTagNameToIsChecked[chipTextAsString] = providedIsChecked
              isChecked = providedIsChecked
              setBackgroundColour(providedIsChecked)
              if (isChecked) {
                thisWebsiteEntryCustomTags.add(chipTextAsString)
                thisWebsiteEntryCustomTags = thisWebsiteEntryCustomTags.distinct().toMutableList()
              } else {
                thisWebsiteEntryCustomTags.remove(chipTextAsString)
              }
            }
          }
          /* Deleting tag from global list. */
          setOnLongClickListener {
            val chipTextAsString = text.toString()
            globalEntryTagsNames = globalEntryTagsNames - chipTextAsString
            fillTagCloud(oldTagName = chipTextAsString)
            /* Remove this tag from the current WebsiteEntry. */
            thisWebsiteEntryCustomTags.remove(chipTextAsString)
            updateWebsiteEntryCustomTags()
            true
          }
          /* If changed by un/checking in Cloud Tag Editor. */
          apply {
            val chipTextAsString = text.toString()
            val isTagChecked = checkedTagNameToIsChecked[chipTextAsString] == true || thisWebsiteEntryCustomTags.contains(chipTextAsString)
            if (isChecked) {
              thisWebsiteEntryCustomTags.add(chipTextAsString)
              thisWebsiteEntryCustomTags = thisWebsiteEntryCustomTags.distinct().toMutableList()
            } else {
              thisWebsiteEntryCustomTags.remove(chipTextAsString)
            }
            isChecked = isTagChecked
            setBackgroundColour(isTagChecked)
          }
        }
      }
      updateWebsiteEntryCustomTags()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Setting up ViewModel and LiveData
    viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    with(viewModel) {
      /*
        This MUST NOT be in its own proc!
        Gets also triggered, when using the UI for switching to MainActivity.
      */
      getWebSiteEntryList().observe(this@CreateEntryActivity) { observedWebsites ->
        websites = observedWebsites
      }
    }
    var isEntryNew = false
    chipColourIdDefault = Chip(this).chipBackgroundColor //TODO: Get the default colour in a more efficient way.
    activityCreateEntryBinding = ActivityCreateEntryBinding.inflate(layoutInflater)
    setContentView(activityCreateEntryBinding.root)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    /* Prepopulate existing Title and Content from Intent. */
    val intent = intent
    if (intent != null && intent.hasExtra(Constants.INTENT_OBJECT)) {
      webSiteEntry = intent.getParcelableExtra(Constants.INTENT_OBJECT)
      when (webSiteEntry) {
        null -> {
          isEntryNew = true
        }
        else -> {
          /*
            Everything in this block gets only executed, if we are editing an already existing entry.
            If we are creating a new entry, this whole block is omitted.
          */
          val entry: WebSiteEntry = webSiteEntry ?: throw IllegalStateException(msgNullNotNull)
          prePopulateUIwithData(entry)
          thisWebsiteEntryCustomTags = entry.customTags.toMutableList()
        }
      }
    } else {
      isEntryNew = true
    }
    viewBtnSave = activityCreateEntryBinding.btnSave
    fillTagCloud(init = true)

    title = if (webSiteEntry != null) getString(R.string.update_entry) else getString(R.string.create_entry)

    if (activityCreateEntryBinding.editUrl.text?.isBlank() == true) {
      activityCreateEntryBinding.editUrl.text.let {
        it?.clear()
        it?.append(prefixHttps)
      }
    }

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

    with (activityCreateEntryBinding) {
      when {
        checkDNSRecords.isChecked -> {
          hideIsOnionAddress()
        }
        isOnionAddress.isChecked -> {
          hideCheckDNSRecords()
          hideIsLaissezFaire()
        }
        isTcpAddress.isChecked -> {
          hideCheckDNSRecords()
          hideIsLaissezFaire()
          hideIsOnionAddress()
          hideIsSmtpAddress()
          hideIsImapAddress()
        }
        isSmtpAddress.isChecked -> {
          hideCheckDNSRecords()
          hideIsLaissezFaire()
          hideIsOnionAddress()
          hideIsTcpAddress()
          hideIsImapAddress()
        }
        isImapAddress.isChecked -> {
          hideCheckDNSRecords()
          hideIsLaissezFaire()
          hideIsOnionAddress()
          hideIsTcpAddress()
          hideIsSmtpAddress()
        }
        else -> {}
      }
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

    activityCreateEntryBinding.isTcpAddress.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        hideCheckDNSRecords()
        hideIsLaissezFaire()
        hideIsOnionAddress()
        hideIsSmtpAddress()
        hideIsImapAddress()
      } else {
        showCheckDNSRecords()
        showIsLaissezFaire()
        showIsOnionAddress()
        showIsSmtpAddress()
        showIsImapAddress()
      }
    }

    activityCreateEntryBinding.isSmtpAddress.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        hideCheckDNSRecords()
        hideIsLaissezFaire()
        hideIsOnionAddress()
        hideIsTcpAddress()
        hideIsImapAddress()
      } else {
        showCheckDNSRecords()
        showIsLaissezFaire()
        showIsOnionAddress()
        showIsTcpAddress()
        showIsImapAddress()
      }
    }

    activityCreateEntryBinding.isImapAddress.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        hideCheckDNSRecords()
        hideIsLaissezFaire()
        hideIsOnionAddress()
        hideIsTcpAddress()
        hideIsSmtpAddress()
      } else {
        showCheckDNSRecords()
        showIsLaissezFaire()
        showIsOnionAddress()
        showIsTcpAddress()
        showIsSmtpAddress()
      }
    }

    activityCreateEntryBinding.checkDNSRecords.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        hideIsOnionAddress()
      } else {
        showIsOnionAddress()
      }
    }

    //region Tag Cloud

    //region Tag Cloud: Display

    activityCreateEntryBinding.entryCreateTagCloud.isSingleSelection = false

    //endregion Tag Cloud: Display

    //region Tag Cloud: Edit

    activityCreateEntryBinding.btnCreateEntryTagCloudEdit.setOnClickListener { viewCreateEntryTagCloudEdit ->
      /** AlertDialog: Manage Tags */
      /** https://stackoverflow.com/a/68973459/7061105 */
      AlertDialog.Builder(this).apply {
        setTitle(R.string.text_create_entry_tag_cloud_edit)
        val checkedTagNamesBefore = thisWebsiteEntryCustomTags.sorted()
        val sortedGlobalEntryTagsNames = globalEntryTagsNames.sorted()
        val checkedItemsBefore = sortedGlobalEntryTagsNames.map { globalTagName ->
          checkedTagNamesBefore.any { checkedTagName -> globalTagName == checkedTagName }
        }.toBooleanArray()
        val checkedTagNamesAfter: MutableList<String> = checkedTagNamesBefore.toMutableList()

        setMultiChoiceItems(
          sortedGlobalEntryTagsNames.toTypedArray(),
          checkedItemsBefore
        ) { _, itemPosition, isChecked ->
          val selectedItemName = sortedGlobalEntryTagsNames[itemPosition]
          fun setToChecked() { checkedTagNameToIsChecked[selectedItemName] = isChecked }
          when (isChecked) {
             true -> {
              setToChecked()
              checkedTagNamesAfter += selectedItemName
              checkedTagNamesAfter.sort()
            }
            false -> {
              setToChecked()
              checkedTagNamesAfter -= selectedItemName
              checkedTagNamesAfter.sort()
            }
          }
        }

        setPositiveButton(
          R.string.text_create_entry_tag_cloud_accept
        ) { _, _ ->
          val checkedTagNames = checkedTagNamesAfter.sorted().toMutableList()
          thisWebsiteEntryCustomTags = checkedTagNames
          updateWebsiteEntryCustomTags()
          checkedTagNames.forEach { checkedTagName ->
            checkedTagNameToIsChecked[checkedTagName] = true
          }
          fillTagCloud()
          viewCreateEntryTagCloudEdit.requestFocus()
//          saveEntry(false)
        }

        setNegativeButton(R.string.text_create_entry_tag_cloud_cancel) { dialog, _ -> dialog.dismiss() }

        setNeutralButton(R.string.text_create_entry_tag_cloud_add) { _, _ ->
          val editText: TextInputEditText = TextInputEditText(this@CreateEntryActivity).apply {
            inputType = InputType.TYPE_CLASS_TEXT
          }.apply EDIT_TEXT@{
            rootView.apply VIEW_EDIT_TEXT@{
              /** https://developer.squareup.com/blog/showing-the-android-keyboard-reliably/ */
              requestFocus()
              val onWindowFocusChangeListener = object : ViewTreeObserver.OnWindowFocusChangeListener {
                override fun onWindowFocusChanged(hasFocus: Boolean) {
                  // This notification will arrive just before the InputMethodManager gets set up.
                  if (hasFocus) {
                    /** Keyboard */
                    this@CreateEntryActivity.showKeyboard(this@VIEW_EDIT_TEXT)
                    // It’s very important to remove this listener once we are done.
                    viewTreeObserver.removeOnWindowFocusChangeListener(this)
                  }
                }
              }
              viewTreeObserver.addOnWindowFocusChangeListener(onWindowFocusChangeListener)
            } /* END: VIEW_EDIT_TEXT */
          } /* END: EDIT_TEXT */
          /** AlertDialog: Add New Tag */
          val dialogBuilderTagAdd = AlertDialog.Builder(this@CreateEntryActivity).apply DIALOG_BUILDER_TAG_ADD@{
            setTitle(R.string.text_create_entry_tag_cloud_add)
            setView(editText)
            setPositiveButton(
              R.string.text_create_entry_tag_cloud_create
            ) { _, _ -> }
            setNegativeButton(
              R.string.text_create_entry_tag_cloud_cancel
            ) { dialog, _ ->
              dialog.dismiss()
            }
          }.create().apply {
            show()
          } /* END: DIALOG_BUILDER_TAG_ADD */
          /* Do not chain! This must be separate and applied to the val `dialogBuilder` or else you will get a null reference. */
          dialogBuilderTagAdd.apply {
            /** https://stackoverflow.com/a/26087003/7061105 */
            /** https://stackoverflow.com/a/27345656/7061105 */
            /** https://stackoverflow.com/a/46742001/7061105 */
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
              val newTagName = editText.text.toString()
              val defSize = amountMaxCharsInNameTag
              if (newTagName.length > defSize) {
                this@CreateEntryActivity.showToast("${defSize} characters maximum!")
              } else {
                globalEntryTagsNames = globalEntryTagsNames + newTagName
                checkedTagNameToIsChecked[newTagName] = false
                fillTagCloud(newTagName = newTagName)
                dismiss()
              }
            }
          }
        } /* END: setNeutralButton() */
        setOnDismissListener {
          viewCreateEntryTagCloudEdit.requestFocus()
          fillTagCloud()
        }
      }.create().show()
    }

    //endregion Tag Cloud: Edit

    //endregion Tag Cloud

    activityCreateEntryBinding.btnSave.setOnClickListener {
      val websiteEntryWithExistingUrl = findWebsiteEntryWithExistingUrl(activityCreateEntryBinding.editUrl.text.toString())
      val urlAlreadyExistsInAnotherWebsiteEntry = websiteEntryWithExistingUrl != null
      if (isEntryNew && urlAlreadyExistsInAnotherWebsiteEntry) {
        AlertDialog.Builder(this).apply {
          setMessage("""The existing Website Entry with the name "${websiteEntryWithExistingUrl?.name}" already checks the URL you provided. """ + getString(R.string.text_add_duplicate_website_entry_url_are_you_sure))
          setTitle(R.string.text_add_duplicate_website_entry_url)
          setPositiveButton(
            R.string.text_delete_all_website_entries_are_you_sure_yes
          ) { _, _ ->
            Log.warn("Confirmed to add Website Entry with duplicate URL!")
            saveEntry()
          }
          setNegativeButton(R.string.text_delete_all_website_entries_are_you_sure_no) { _, _ ->
            Log.warn("Denied to add Website Entry with duplicate URL!")
          }
        }.create().show()
      } else {
        saveEntry()
      }
    }

    if (webSiteEntry != null) {
      /*
        We usually do not want to edit the WebsiteEntry's name.
        So, we want it to not be focused and open a keyboard, without asking for it.
      */
      viewBtnSave.requestFocus()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        alertUnsavedChanges()
        super.onOptionsItemSelected(item)
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onBackPressed() {
    alertUnsavedChanges()
  }

  override fun onPause() {
    super.onPause()
    val remember = remember(this, prefNameMemorisedWebsiteEntryData)
    if(websiteEntryIsBeingSubmitted) {
      remember.saveName = ""
      remember.saveURL = prefixHttps
      remember.saveIsDNSChecked = false
      remember.saveIsLaissezFaireChecked = false
      remember.saveIsOnionChecked = false
      remember.saveIsOnionChecked = false
      remember.saveIsOnionChecked = false
      remember.saveIsOnionChecked = false
      remember.setSaveEntryTags(mutableListOf())
    } else {
      remember.saveName = activityCreateEntryBinding.editName.text.toString()
      remember.saveURL = activityCreateEntryBinding.editUrl.text.toString()
      remember.saveIsDNSChecked = activityCreateEntryBinding.checkDNSRecords.isChecked
      remember.saveIsLaissezFaireChecked = activityCreateEntryBinding.isLaissezFaire.isChecked
      remember.saveIsOnionChecked = activityCreateEntryBinding.isOnionAddress.isChecked
      remember.saveIsTcpChecked = activityCreateEntryBinding.isTcpAddress.isChecked
      remember.saveIsSmtpChecked = activityCreateEntryBinding.isSmtpAddress.isChecked
      remember.saveIsImapChecked = activityCreateEntryBinding.isImapAddress.isChecked
      remember.setSaveEntryTags(thisWebsiteEntryCustomTags)
    }
  }

  override fun onResume() {
    super.onResume()
    if (webSiteEntry != null) { return }
    val remember = remember(this, prefNameMemorisedWebsiteEntryData)
    with (activityCreateEntryBinding) {
      editName.setText(remember.saveName)
      editUrl.setText(remember.saveURL)
      checkDNSRecords.isChecked = remember.saveIsDNSChecked
      isLaissezFaire.isChecked = remember.saveIsLaissezFaireChecked
      isOnionAddress.isChecked = remember.saveIsOnionChecked
      isTcpAddress.isChecked = remember.saveIsTcpChecked
      isSmtpAddress.isChecked = remember.saveIsSmtpChecked
      isImapAddress.isChecked = remember.saveIsImapChecked
      thisWebsiteEntryCustomTags = remember.getSaveEntryTags().toMutableList()
    }
    updateWebsiteEntryCustomTags()
    fillTagCloud()
    websiteEntryIsBeingSubmitted = false
  }

  private fun prePopulateUIwithData(previousVersionWebsiteEntry: WebSiteEntry) {
    /*
      After adding UI elements which directly correspond to an additional property in the `WebSiteEntry` data class,
      CTRL+SHIFT+F for "WebsiteEntry Glue" and update the `WebSiteEntry` initialisations accordingly.
    */
    val isOnion = previousVersionWebsiteEntry.isOnionAddress
    val isTCP = previousVersionWebsiteEntry.isTcpAddress
    val isSMTP = previousVersionWebsiteEntry.isSmtpAddress
    val isIMAP = previousVersionWebsiteEntry.isImapAddress
    fun Boolean.ifNotOnion(): Boolean = if (isOnion) { false } else { this }
    activityCreateEntryBinding.editName.setText(previousVersionWebsiteEntry.name)
    activityCreateEntryBinding.editUrl.setText(previousVersionWebsiteEntry.url)
    activityCreateEntryBinding.isLaissezFaire.isChecked = previousVersionWebsiteEntry.isLaissezFaire.ifNotOnion()
    activityCreateEntryBinding.checkDNSRecords.isChecked = previousVersionWebsiteEntry.dnsRecordsAAAAA.ifNotOnion()
    activityCreateEntryBinding.isOnionAddress.isChecked = isOnion /* No `if` check, because it would cause more harm to reset it, than to leave it be. */
    activityCreateEntryBinding.isTcpAddress.isChecked = isTCP
    activityCreateEntryBinding.isSmtpAddress.isChecked = isSMTP
    activityCreateEntryBinding.isImapAddress.isChecked = isIMAP
    activityCreateEntryBinding.btnSave.text = getString(R.string.update)

  }


  /**
   * Sends the updated information back to calling Activity
   * */
  private fun saveEntry(doFinish: Boolean = true) {
    if (validateFields()) {
      isEntryCreated = true
      val id = webSiteEntry?.id
      val todo = WebSiteEntry(
        /* WebsiteEntry Glue */
        id = id,
        name = activityCreateEntryBinding.editName.text.toString(),
        url = activityCreateEntryBinding.editUrl.text.toString(),
        status = webSiteEntry?.status,
        updatedAt = webSiteEntry?.updatedAt,
        itemPosition = if (webSiteEntry != null) { webSiteEntry?.itemPosition } else { totalAmountEntry },
        dnsRecordsAAAAA = activityCreateEntryBinding.checkDNSRecords.isChecked,
        isOnionAddress = activityCreateEntryBinding.isOnionAddress.isChecked,
        isTcpAddress = activityCreateEntryBinding.isTcpAddress.isChecked,
        isSmtpAddress = activityCreateEntryBinding.isSmtpAddress.isChecked,
        isImapAddress = activityCreateEntryBinding.isImapAddress.isChecked,
        isLaissezFaire = activityCreateEntryBinding.isLaissezFaire.isChecked,
        customTags = thisWebsiteEntryCustomTags
      )
      Log.info("WebsiteEntry after Edit: " + todo)
      val intent = Intent()
      intent.putExtra(Constants.INTENT_OBJECT, todo)
      setResult(RESULT_OK, intent)
      websiteEntryIsBeingSubmitted = true
      if (doFinish) { finish() }
    }
  }

  /**
   * Validation of EditText
   * */
  private fun validateFields(): Boolean {
    if (activityCreateEntryBinding.editName.text?.isEmpty() == true) {
      activityCreateEntryBinding.inputName.error = getString(R.string.enter_valid_name)
      activityCreateEntryBinding.editName.requestFocus()
      return false
    }
    if (activityCreateEntryBinding.editUrl.text?.isEmpty() == true) {
      activityCreateEntryBinding.inputUrl.error = getString(R.string.enter_valid_url)
      activityCreateEntryBinding.editUrl.requestFocus()
      return false
    }
    return true
  }

  private fun isWebsiteEntryTainted(): Boolean {
    return when (webSiteEntry) {
      null -> {
        when {
          activityCreateEntryBinding.editName.text?.isEmpty() == true ||
          activityCreateEntryBinding.editUrl.text?.isEmpty() == true -> {
            false
          } else -> true
        }
      }
      else -> {
        with (webSiteEntry!!) {
          when {
            name == activityCreateEntryBinding.editName.text.toString() &&
            url == activityCreateEntryBinding.editUrl.text.toString() &&
            status == webSiteEntry?.status &&
            updatedAt == webSiteEntry?.updatedAt &&
            itemPosition == webSiteEntry?.itemPosition &&
            dnsRecordsAAAAA == activityCreateEntryBinding.checkDNSRecords.isChecked &&
            isOnionAddress == activityCreateEntryBinding.isOnionAddress.isChecked &&
            isTcpAddress == activityCreateEntryBinding.isTcpAddress.isChecked &&
            isSmtpAddress == activityCreateEntryBinding.isSmtpAddress.isChecked &&
            isImapAddress == activityCreateEntryBinding.isImapAddress.isChecked &&
            isLaissezFaire == activityCreateEntryBinding.isLaissezFaire.isChecked &&
            customTags == thisWebsiteEntryCustomTags -> {
              false
            }
            else -> true
          }
        }
      }
    }
  }

  private fun Context.alertUnsavedChanges() {
    if (isWebsiteEntryTainted().not()) {
      websiteEntryIsBeingSubmitted = true
      super.onBackPressed()
      return
    }
    AlertDialog.Builder(this).apply {
      setMessage("""Do you want to discard Website Entry data changes?""")
      setTitle("""Unsaved Changes Detected""")
      setPositiveButton(
        R.string.text_delete_all_website_entries_are_you_sure_yes
      ) { _, _ ->
        Log.warn("Confirmed to discard Website Entry data changes!")
        websiteEntryIsBeingSubmitted = true
        super.onBackPressed()
      }
      setNegativeButton(R.string.text_delete_all_website_entries_are_you_sure_no) { _, _ ->
        Log.warn("Denied to discard Website Entry data changes!")
      }
    }.create().show()
  }
}
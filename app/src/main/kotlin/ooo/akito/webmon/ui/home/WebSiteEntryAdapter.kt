package ooo.akito.webmon.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.PopupMenu
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import ooo.akito.webmon.R
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.databinding.ItemWebsiteRowBinding
import ooo.akito.webmon.utils.Print
import ooo.akito.webmon.utils.Utils
import ooo.akito.webmon.utils.Utils.currentDateTime
import ooo.akito.webmon.utils.Utils.removeUrlProto
import java.util.*

/**
 * @author Naveen T P
 * @since 08/11/18
 */
class WebSiteEntryAdapter(todoEvents: WebSiteEntryEvents) : RecyclerView.Adapter<WebSiteEntryAdapter.ViewHolder>(), Filterable {

    private var mList: List<WebSiteEntry> = arrayListOf()
    private var filteredList: List<WebSiteEntry> = arrayListOf()
    private val listener: WebSiteEntryEvents = todoEvents
    private lateinit var itemWebsiteRowBinding: ItemWebsiteRowBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        itemWebsiteRowBinding = ItemWebsiteRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemWebsiteRowBinding)
    }

    override fun getItemCount(): Int = filteredList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with (holder) {
            with (filteredList[position]) {
                holder.itemView.tag = this
                holder.bind(this, listener, position)
            }
        }
    }

    inner class ViewHolder(val binding: ItemWebsiteRowBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(webSiteEntry: WebSiteEntry, listener: WebSiteEntryEvents, position: Int) {

            binding.root.apply {

                binding.txtWebSite.text = webSiteEntry.name
                binding.txtUrl.text = webSiteEntry.url

                /** https://gitlab.com/fdroid/fdroiddata/-/merge_requests/10001#note_720175502 */
//                val iconUrl = "https://icons.duckduckgo.com/ip3/${webSiteEntry.url.removeUrlProto()}.ico"
//                try {
//                    Glide.with(binding.imgLogo.context).load(iconUrl).apply(RequestOptions.circleCropTransform()).into(binding.imgLogo)
//                } catch (e: Exception) {
//                    Print.log(e.message ?: "Exception occured when using Glide to load Website Logo.")
//                }


                binding.txtStatus.text = HtmlCompat.fromHtml("<b>Status :</b> ${webSiteEntry.status ?: "000"} - ${Utils.getStatusMessage(webSiteEntry.status)}<br><b>Last Update :</b> ${webSiteEntry.updatedAt ?: currentDateTime()}", HtmlCompat.FROM_HTML_MODE_LEGACY)
                binding.imgIndicator.setImageResource(if(webSiteEntry.status != 200) R.drawable.ic_alert else R.drawable.ic_success)
                binding.btnPause.setImageResource(if(webSiteEntry.isPaused) R.drawable.ic_play else R.drawable.ic_pause)


                val popupMenu = PopupMenu(context, binding.btnMore)
                popupMenu.inflate(R.menu.menu_website_more)
                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_refresh -> listener.onRefreshClicked(webSiteEntry)
                        R.id.action_visit -> listener.onVisitClicked(webSiteEntry)
                        R.id.action_edit -> listener.onEditClicked(webSiteEntry)
                        R.id.action_delete -> listener.onDeleteClicked(webSiteEntry)
                    }
                    true
                }

                binding.btnMore.setOnClickListener {
                    try {
                        val popup = PopupMenu::class.java.getDeclaredField("mPopup")
                        popup.isAccessible =  true
                        val menu = popup.get(popupMenu)
                        menu.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(menu, true)
                    }catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        popupMenu.show()
                    }
                }

                binding.btnPause.setOnClickListener {
                    listener.onPauseClicked(webSiteEntry, position)
                    webSiteEntry.isPaused = webSiteEntry.isPaused.not()
                    notifyItemChanged(position)
                }


                this.setOnClickListener { listener.onRefreshClicked(webSiteEntry) }
                this.setOnLongClickListener {
                    listener.onViewClicked(webSiteEntry, position)
                    notifyItemChanged(position)
                    true
                }
            }
        }
    }

    /**
     * Search Filter implementation
     * */
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(p0: CharSequence?): FilterResults {
                val charString = p0.toString()
                filteredList = if (charString.isEmpty()) {
                    mList
                } else {
                    val filteredList = arrayListOf<WebSiteEntry>()
                    for (row in mList) {
                        if (
                            row.name
                                .lowercase(Locale.getDefault())
                                .contains(charString.lowercase(Locale.getDefault()))
                            || row.url.contains(charString.lowercase(Locale.getDefault()))
                        ) {
                            filteredList.add(row)
                        }
                    }
                    filteredList
                }

                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                filteredList = (p1?.values as List<*>).filterIsInstance<WebSiteEntry>()
                notifyDataSetChanged()
            }

        }
    }

    /**
     * Activity uses this method to update todoList with the help of LiveData
     * */
    fun setAllTodoItems(todoItems: List<WebSiteEntry>) {
        this.mList = todoItems
        this.filteredList = todoItems.sortedBy { it.itemPosition }
        notifyDataSetChanged()
    }

    /**
     * RecycleView touch event callbacks
     * */
    interface WebSiteEntryEvents {
        fun onDeleteClicked(webSiteEntry: WebSiteEntry)
        fun onViewClicked(webSiteEntry: WebSiteEntry, adapterPosition: Int)
        fun onVisitClicked(webSiteEntry: WebSiteEntry)
        fun onEditClicked(webSiteEntry: WebSiteEntry)
        fun onRefreshClicked(webSiteEntry: WebSiteEntry)
        fun onPauseClicked(webSiteEntry: WebSiteEntry, adapterPosition: Int)
    }
}

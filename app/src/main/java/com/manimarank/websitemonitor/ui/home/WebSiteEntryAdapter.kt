package com.manimarank.websitemonitor.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.PopupMenu
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.manimarank.websitemonitor.R
import com.manimarank.websitemonitor.data.db.WebSiteEntry
import com.manimarank.websitemonitor.utils.Utils.currentDateTime
import kotlinx.android.synthetic.main.item_website_row.view.*
import java.util.*

/**
 * @author Naveen T P
 * @since 08/11/18
 */
class WebSiteEntryAdapter(todoEvents: WebSiteEntryEvents) : RecyclerView.Adapter<WebSiteEntryAdapter.ViewHolder>(), Filterable {

    private var mList: List<WebSiteEntry> = arrayListOf()
    private var filteredList: List<WebSiteEntry> = arrayListOf()
    private val listener: WebSiteEntryEvents = todoEvents

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_website_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = filteredList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredList[position], listener, position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bind(webSiteEntry: WebSiteEntry, listener: WebSiteEntryEvents, position: Int) {

            itemView.apply {

                txtWebSite.text = webSiteEntry.name
                txtUrl.text = webSiteEntry.url

                val iconUrl = "https://www.google.com/s2/favicons?domain=${webSiteEntry.url}"
                Glide.with(itemView.imgLogo.context).load(iconUrl).apply(RequestOptions.circleCropTransform()).into(itemView.imgLogo)

                txtStatus.text = HtmlCompat.fromHtml("<b>Status :</b> ${webSiteEntry.status ?: "---"}<br><b>Last Update :</b> ${webSiteEntry.updatedAt ?: currentDateTime()}", HtmlCompat.FROM_HTML_MODE_LEGACY)
                imgIndicator.setImageResource(if(webSiteEntry.status != 200) R.drawable.ic_alert else R.drawable.ic_success)
                btnPause.setImageResource(if(webSiteEntry.isPaused) R.drawable.ic_play else R.drawable.ic_pause)


                val popupMenu = PopupMenu(context, btnMore)
                popupMenu.inflate(R.menu.menu_website_more)
                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_refresh -> listener.onRefreshClicked(webSiteEntry)
                        R.id.action_visit -> listener.onViewClicked(webSiteEntry)
                        R.id.action_edit -> listener.onEditClicked(webSiteEntry)
                        R.id.action_delete -> listener.onDeleteClicked(webSiteEntry)
                    }
                    true
                }

                btnMore.setOnClickListener {
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

                btnPause.setOnClickListener {
                    listener.onPauseClicked(webSiteEntry, position)
                    webSiteEntry.isPaused = webSiteEntry.isPaused.not()
                    notifyItemChanged(position)
                }


                this.setOnClickListener { listener.onRefreshClicked(webSiteEntry) }
                this.setOnLongClickListener {
                    listener.onEditClicked(webSiteEntry)
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
                        if (row.name.toLowerCase(Locale.getDefault()).contains(charString.toLowerCase(Locale.getDefault()))
                            || row.url.contains(charString.toLowerCase(Locale.getDefault()))
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
                filteredList = p1?.values as List<WebSiteEntry>
                notifyDataSetChanged()
            }

        }
    }

    /**
     * Activity uses this method to update todoList with the help of LiveData
     * */
    fun setAllTodoItems(todoItems: List<WebSiteEntry>) {
        this.mList = todoItems
        this.filteredList = todoItems
        notifyDataSetChanged()
    }

    /**
     * RecycleView touch event callbacks
     * */
    interface WebSiteEntryEvents {
        fun onDeleteClicked(webSiteEntry: WebSiteEntry)
        fun onViewClicked(webSiteEntry: WebSiteEntry)
        fun onEditClicked(webSiteEntry: WebSiteEntry)
        fun onRefreshClicked(webSiteEntry: WebSiteEntry)
        fun onPauseClicked(webSiteEntry: WebSiteEntry, adapterPosition: Int)
    }
}

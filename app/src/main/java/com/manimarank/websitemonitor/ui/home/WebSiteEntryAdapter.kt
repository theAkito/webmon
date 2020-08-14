package com.manimarank.websitemonitor.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
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
        holder.bind(filteredList[position], listener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bind(webSiteEntry: WebSiteEntry, listener: WebSiteEntryEvents) {
            itemView.txtWebSite.text = webSiteEntry.name
            itemView.txtUrl.text = webSiteEntry.url

            itemView.txtStatus.text = HtmlCompat.fromHtml("<b>Status :</b> ${webSiteEntry.status ?: "---"}<br><b>Last Update :</b> ${webSiteEntry.updatedAt ?: currentDateTime()}", HtmlCompat.FROM_HTML_MODE_LEGACY)

            itemView.imgIndicator.setImageResource(if(webSiteEntry.status != 200) R.drawable.ic_alert else R.drawable.ic_success)

            itemView.imgPlayPause.setOnClickListener {
                listener.onDeleteClicked(webSiteEntry)
            }

            itemView.setOnClickListener {
                listener.onViewClicked(webSiteEntry)
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
    }
}

package ooo.akito.webmon.data.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Entity(tableName = "web_site_entry")
@Parcelize
data class WebSiteEntry(
  @PrimaryKey(autoGenerate = true)
  val id: Long? = null,
  @ColumnInfo(name = "name")
  val name: String,
  @ColumnInfo(name = "url")
  val url: String,
  @ColumnInfo(name = "status")
  var status: Int? = null,
  @ColumnInfo(name = "is_paused")
  var isPaused: Boolean = false,
  @ColumnInfo(name = "updated_at")
  var updatedAt: String? = null,
  @ColumnInfo(name = "item_position")
  var itemPosition: Int? = null,
  @ColumnInfo(name = "is_laissez_faire", defaultValue = false.toString())
  var isLaissezFaire: Boolean = false,
  @ColumnInfo(name = "dns_records_a_aaaa", defaultValue = false.toString())
  var dnsRecordsAAAAA: Boolean = false,
  @ColumnInfo(name = "is_onion_address", defaultValue = false.toString())
  var isOnionAddress: Boolean = false
) : Parcelable
package ooo.akito.webmon.data.db

import androidx.room.TypeConverter
import com.fasterxml.jackson.module.kotlin.readValue
import ooo.akito.webmon.utils.jString
import ooo.akito.webmon.utils.mapper

abstract class DatabaseTypeConvertorBase<T> {
  /** https://stackoverflow.com/a/67921091/7061105 */
  @TypeConverter fun mapListToJString(list: List<T>): String = mapper.writeValueAsString(list)
  @TypeConverter fun mapJStringToList(listAsJString: jString): List<T> = try { mapper.readValue(listAsJString) } catch (e: Exception) { emptyList() }
}
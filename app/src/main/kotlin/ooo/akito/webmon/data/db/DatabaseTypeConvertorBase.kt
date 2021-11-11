package ooo.akito.webmon.data.db

import androidx.room.TypeConverter
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.reflect.TypeToken
import ooo.akito.webmon.utils.Utils.mapper
import ooo.akito.webmon.utils.jString

abstract class DatabaseTypeConvertorBase<T> {
  /** https://stackoverflow.com/a/67921091/7061105 */
  @TypeConverter fun mapListToJString(list: List<T>): String = mapper.writeValueAsString(list)
  @TypeConverter fun mapJStringToList(listAsJString: jString): List<T> = try { mapper.readValue(listAsJString) } catch (e: Exception) { emptyList() }
}
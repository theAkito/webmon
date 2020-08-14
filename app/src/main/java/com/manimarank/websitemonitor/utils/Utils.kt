package com.manimarank.websitemonitor.utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

object Utils {
    fun currentDateTime(): String {
        return SimpleDateFormat("dd-MMM-yyyy HH:mm:ss:a", Locale.ENGLISH).format(Date())
    }
}
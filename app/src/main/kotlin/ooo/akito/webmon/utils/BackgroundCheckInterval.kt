package ooo.akito.webmon.utils

object BackgroundCheckInterval {
  /**
    Setting to less than 15 minutes is not possible, due to internal hard limit.
    https://stackoverflow.com/questions/55135999/how-to-reduce-time-of-periodicworkmanager-in-workmanager
  */
  val valueList = arrayOf(
    15,
    20,
    30,
    45,
    60,
    60 * 2,
    60 * 3,
    60 * 4,
    60 * 5,
    60 * 6,
    60 * 8,
    60 * 10,
    60 * 12,
    60 * 24,
    60 * 24 * 2,
    60 * 24 * 3,
    60 * 24 * 7
  )
  val nameList = arrayOf(
    "Every ${valueList[0]} minutes",
    "Every ${valueList[1]} minutes",
    "Every ${valueList[2]} minutes",
    "Every ${valueList[3]} minutes",
    "Every hour", /* 1h */
    "Every ${valueList[5].toHours()} hours", /* 2h */
    "Every ${valueList[6].toHours()} hours", /* 3h */
    "Every ${valueList[7].toHours()} hours", /* 4h */
    "Every ${valueList[8].toHours()} hours", /* 5h */
    "Every ${valueList[9].toHours()} hours", /* 6h */
    "Every ${valueList[10].toHours()} hours", /* 8h */
    "Every ${valueList[11].toHours()} hours", /* 10h */
    "Every ${valueList[12].toHours()} hours", /* 12h */
    "Every day", /* 1d */
    "Every second day", /* 2d */
    "Every third day", /* 3d */
    "Weekly"
  )
  private fun Int.toHours(): Int {
    return this / 60
  }
  private fun Int.toHoursAndMinutes(): Pair<Int, Int> {
    return this.toHours() to this % 60
  }
}
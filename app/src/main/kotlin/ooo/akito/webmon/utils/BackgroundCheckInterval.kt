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
    60 * 12,
    60 * 24,
    60 * 24 * 7
  )
  val nameList = arrayOf(
    "Every ${valueList[0]} minutes",
    "Every ${valueList[1]} minutes",
    "Every ${valueList[2]} minutes",
    "Every ${valueList[3]} minutes",
    "Every ${valueList[4]} minutes",
    "Every ${valueList[5]} minutes",
    "Every ${valueList[6]} minutes",
    "Every ${valueList[7]} minutes",
    "Every ${valueList[8]} minutes",
    "Every ${valueList[9]} minutes",
    "Every ${valueList[10]} minutes",
    "Every ${valueList[11]} minutes",
    "Weekly"
  )
}
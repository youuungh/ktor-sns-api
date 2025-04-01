package com.ninezero.utils

import java.time.format.DateTimeFormatter

object DateUtils {
    const val DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS"
    val formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)
}
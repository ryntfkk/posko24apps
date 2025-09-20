package com.example.posko24.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

val APP_LOCALE: Locale = Locale("id", "ID")
private val jakartaTimeZone: TimeZone = TimeZone.getTimeZone(APP_TIME_ZONE_ID)

fun appSimpleDateFormat(pattern: String, locale: Locale = APP_LOCALE): SimpleDateFormat {
    return SimpleDateFormat(pattern, locale).apply {
        timeZone = jakartaTimeZone
    }
}
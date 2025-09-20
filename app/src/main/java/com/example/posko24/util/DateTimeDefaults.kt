package com.example.posko24.util

import java.time.ZoneId
import kotlinx.datetime.TimeZone

const val APP_TIME_ZONE_ID: String = "Asia/Jakarta"
val APP_ZONE_ID: ZoneId = ZoneId.of(APP_TIME_ZONE_ID)
val APP_TIME_ZONE: TimeZone = TimeZone.of(APP_TIME_ZONE_ID)
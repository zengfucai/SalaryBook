
package com.salarybook.domain.salary

import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 单段工作时段。
 *
 * overtimeType:
 * NONE       正常工作
 * WEEKDAY    平时加班
 * REST_DAY   休息日加班
 * HOLIDAY    法定节假日加班
 */
data class WorkSegment(
    val id: String,
    val startTime: String,
    val endTime: String,
    val isOvertime: Boolean = false,
    val overtimeType: String = "NONE"
) {
    fun durationMinutes(): Int {
        return try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val start = LocalTime.parse(startTime, formatter)
            val end = LocalTime.parse(endTime, formatter)

            var minutes = Duration.between(start, end).toMinutes()

            // 结束时间早于开始时间，按次日结束计算
            if (minutes < 0) {
                minutes += 24 * 60
            }

            minutes.coerceAtLeast(0).toInt()
        } catch (_: Exception) {
            0
        }
    }

    fun actualOvertimeType(): String {
        return if (isOvertime) {
            when (overtimeType) {
                "WEEKDAY", "REST_DAY", "HOLIDAY" -> overtimeType
                else -> "WEEKDAY"
            }
        } else {
            "NONE"
        }
    }
}

object WorkSegmentJson {

    fun encode(segments: List<WorkSegment>): String {
        val array = JSONArray()

        segments.forEach { segment ->
            val item = JSONObject().apply {
                put("id", segment.id)
                put("startTime", segment.startTime)
                put("endTime", segment.endTime)
                put("isOvertime", segment.isOvertime)
                put("overtimeType", segment.actualOvertimeType())
            }

            array.put(item)
        }

        return array.toString()
    }

    fun decode(json: String?): List<WorkSegment> {
        if (json.isNullOrBlank()) return emptyList()

        return try {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue

                    val start = item.optString("startTime", "")
                    val end = item.optString("endTime", "")

                    if (!isValidTime(start) || !isValidTime(end)) {
                        continue
                    }

                    val isOvertime = item.optBoolean("isOvertime", false)

                    add(
                        WorkSegment(
                            id = item.optString(
                                "id",
                                "segment_$index"
                            ),
                            startTime = start,
                            endTime = end,
                            isOvertime = isOvertime,
                            overtimeType = if (isOvertime) {
                                item.optString(
                                    "overtimeType",
                                    "WEEKDAY"
                                )
                            } else {
                                "NONE"
                            }
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun isValidTime(value: String): Boolean {
        return try {
            LocalTime.parse(
                value,
                DateTimeFormatter.ofPattern("HH:mm")
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}

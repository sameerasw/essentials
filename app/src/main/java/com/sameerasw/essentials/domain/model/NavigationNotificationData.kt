package com.sameerasw.essentials.domain.model

import android.app.PendingIntent
import android.graphics.Bitmap

data class NavigationNotificationData(
    val key: String,
    val packageName: String,
    val instruction: String?,
    val distance: String?,
    val eta: String?,
    val maneuver: Bitmap?,
    val contentIntent: PendingIntent?,
    val exitIntent: PendingIntent?,
    val postTime: Long,
)

object NavigationParser {
    data class Parsed(val instruction: String?, val distance: String?, val eta: String?)

    private val unitMeters = mapOf(
        "km" to 1000.0, "m" to 1.0, "mi" to 1609.344, "ft" to 0.3048, "yd" to 0.9144,
        "км" to 1000.0, "м" to 1.0,
        "公里" to 1000.0, "千米" to 1000.0, "公尺" to 1.0, "米" to 1.0,
    )
    private val units = unitMeters.keys.sortedByDescending { it.length }.joinToString("|")
    private val distanceRegex = Regex("""\d[\d.,]*\s?($units)(?!\p{L})""", RegexOption.IGNORE_CASE)
    private val exitRegex = Regex(
        """\b(exit|stop|end|close|quit|salir|terminar|detener|finalizar|beenden|quitter|arrêter|sair|encerrar)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun isDistance(s: String?) = s != null && s.length <= 24 && distanceRegex.containsMatchIn(s)

    fun parse(title: String?, text: String?, subText: String?, chip: String?): Parsed {
        val titleIsDistance = isDistance(title)
        val textIsDistance = !titleIsDistance && isDistance(text)
        val instruction = when {
            titleIsDistance -> text
            textIsDistance -> title
            else -> title ?: text
        }
        val leftover = if (!titleIsDistance && !textIsDistance && title != null) text else null
        val distance = chip?.takeIf { it.isNotBlank() } ?: when {
            titleIsDistance -> title
            textIsDistance -> text
            else -> null
        }
        return Parsed(
            instruction = instruction?.takeIf { it.isNotBlank() },
            distance = distance,
            eta = (subText ?: leftover)?.takeIf { it.isNotBlank() },
        )
    }

    private val meterRegex = Regex("""(\d{1,3}(?:,\d{3})+|\d+(?:[.,]\d+)?)\s?($units)(?!\p{L})""", RegexOption.IGNORE_CASE)

    fun meters(distance: String?): Double? {
        val match = meterRegex.find(distance ?: return null) ?: return null
        val (number, unit) = match.destructured
        val value = if (Regex("""\d{1,3}(,\d{3})+""").matches(number)) number.replace(",", "") else number.replace(',', '.')
        return value.toDoubleOrNull()?.times(unitMeters.getValue(unit.lowercase()))
    }

    fun exitActionIndex(titles: List<String?>): Int =
        titles.indexOfFirst { it != null && exitRegex.containsMatchIn(it) }
}

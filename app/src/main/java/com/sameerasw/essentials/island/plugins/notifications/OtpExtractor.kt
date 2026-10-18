package com.sameerasw.essentials.island.plugins.notifications

object OtpExtractor {
    private val KEYWORD = Regex(
        "\\b(otp|code|codes|passcode|pin|2fa|mfa|verification|verify|one[- ]time|" +
            "c[oó]digo|clave|verificaci[oó]n|contrase[nñ]a|senha|verifica[cç][aã]o|" +
            "bestätigungscode|sicherheitscode|vérification|codice|token|nip)\\b",
        RegexOption.IGNORE_CASE,
    )

    private val CANDIDATES = listOf(
        Regex("\\b[A-Z]{1,3}-(\\d{4,8})\\b"),
        Regex("(?<![\\d-])(\\d{3,4}[ -]\\d{3,4})(?![\\d-])"),
        Regex("\\b(\\d{4,8})\\b"),
        Regex("\\b((?=[A-Z0-9]*\\d)(?=[A-Z0-9]*[A-Z])[A-Z0-9]{4,8})\\b"),
    )

    private val DOMAIN_BOUND =
        Regex("@[\\w.-]+\\.[a-z]{2,}\\s+#((?=[A-Za-z0-9]*\\d)[A-Za-z0-9]{4,10})\\b", RegexOption.IGNORE_CASE)

    private val CURRENCY_BEFORE = Regex(
        "([$€£¥₹#]|\\b(usd|mxn|eur|gbp|cop|ars|clp|pen|brl|rs|inr|no|nr|num)\\.?)\\s*$",
        RegexOption.IGNORE_CASE,
    )

    private val CURRENCY_AFTER = Regex(
        "^\\s*([$€£¥₹]|(usd|mxn|eur|gbp|cop|ars|clp|pen|brl|rs|inr|pesos|dollars|d[oó]lares|reais)\\b)",
        RegexOption.IGNORE_CASE,
    )

    private val NOT_A_CODE_BEFORE = Regex(
        "\\b(order|pedido|orden|ref|reference|referencia|account|cuenta|card|tarjeta|ending( in)?|" +
            "terminaci[oó]n|terminada en|tracking|gu[ií]a|invoice|factura|ticket|flight|vuelo|seat|" +
            "promo|promotional|coupon|cup[oó]n|discount|descuento|referral|gift)\\b[^\\d]{0,12}$",
        RegexOption.IGNORE_CASE,
    )

    private val KEYWORD_RIGHT_BEFORE = Regex("(${KEYWORD.pattern})\\s*(is|es|é|:)?\\s*$", RegexOption.IGNORE_CASE)

    private const val MAX_KEYWORD_DISTANCE = 120

    fun extract(text: String): String? {
        if (text.isBlank()) return null
        DOMAIN_BOUND.find(text)?.let { return it.groupValues[1] }
        val keywords = KEYWORD.findAll(text).map { it.range }.toList()
        if (keywords.isEmpty()) return null

        val seen = mutableSetOf<IntRange>()
        var best: Pair<String, Int>? = null
        for (pattern in CANDIDATES) {
            for (match in pattern.findAll(text)) {
                val group = match.groups[1] ?: continue
                if (seen.any { it.overlaps(group.range) }) continue
                seen += group.range
                if (!looksLikeCode(text, group.range, group.value)) continue
                val distance = keywords.minOf { distance(it, group.range) }
                if (distance > MAX_KEYWORD_DISTANCE) continue
                if (best == null || distance < best.second) best = normalize(group.value) to distance
            }
        }
        return best?.first
    }

    private fun looksLikeCode(text: String, range: IntRange, value: String): Boolean {
        val before = text.substring(maxOf(0, range.first - 24), range.first)
        val after = text.substring(range.last + 1, minOf(text.length, range.last + 4))
        if (CURRENCY_BEFORE.containsMatchIn(before)) return false
        if (CURRENCY_AFTER.containsMatchIn(text.substring(range.last + 1, minOf(text.length, range.last + 12)))) return false
        if (NOT_A_CODE_BEFORE.containsMatchIn(before)) return false
        if (after.matches(Regex("^[.,:/]\\d.*", RegexOption.DOT_MATCHES_ALL))) return false
        if (after.startsWith("%")) return false
        if (before.matches(Regex(".*\\d[.,:/]$", RegexOption.DOT_MATCHES_ALL))) return false
        val digits = value.filter { it.isDigit() }
        if (digits.length == value.length && digits.length == 4 && digits.toInt() in 1900..2099) {
            if (!KEYWORD_RIGHT_BEFORE.containsMatchIn(before)) return false
        }
        return true
    }

    private fun IntRange.overlaps(other: IntRange) = first <= other.last && other.first <= last

    private fun distance(a: IntRange, b: IntRange): Int = when {
        a.overlaps(b) -> 0
        a.last < b.first -> b.first - a.last
        else -> a.first - b.last
    }

    private fun normalize(value: String) = value.filterNot { it == ' ' || it == '-' }
}

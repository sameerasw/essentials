package com.sameerasw.essentials.utils

data class SimInfo(
    val id: Int,
    val slotIndex: Int,
    val displayName: String
)

object TelephonyParser {
    fun parseActiveSims(output: String): List<SimInfo> {
        val list = mutableListOf<SimInfo>()
        var inActiveList = false
        output.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (line.contains("Active subscriptions:")) {
                inActiveList = true
            } else if (inActiveList && (trimmed.startsWith("Embedded subscriptions:") || trimmed.startsWith("All Subscription Info List:") || trimmed.startsWith("getAvailableSubscriptionInfoList:"))) {
                inActiveList = false
            }
            
            if (inActiveList && line.contains("SubscriptionInfoInternal:")) {
                val id = parseValue(line, "id=")?.toIntOrNull()
                val simSlotIndex = parseValue(line, "simSlotIndex=")?.toIntOrNull()
                val displayName = parseValue(line, "displayName=") ?: "SIM"
                
                if (id != null && simSlotIndex != null && simSlotIndex >= 0) {
                    if (list.none { it.id == id }) {
                        list.add(SimInfo(id, simSlotIndex, displayName))
                    }
                }
            }
        }
        return list.sortedBy { it.slotIndex }
    }

    private fun parseValue(line: String, key: String): String? {
        val startIndex = line.indexOf(key)
        if (startIndex == -1) return null
        val valStart = startIndex + key.length
        
        var valEnd = valStart
        while (valEnd < line.length) {
            val c = line[valEnd]
            if (c == ' ' || c == ',' || c == ']') {
                break
            }
            valEnd++
        }
        if (valEnd > valStart) {
            return line.substring(valStart, valEnd).trim()
        }
        return null
    }
}

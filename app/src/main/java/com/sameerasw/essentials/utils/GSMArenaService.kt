package com.sameerasw.essentials.utils

import com.sameerasw.essentials.data.model.DeviceSpecCategory
import com.sameerasw.essentials.data.model.DeviceSpecItem
import com.sameerasw.essentials.data.model.DeviceSpecs
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object GSMArenaService {
    private const val BASE_URL = "https://www.gsmarena.com"

    fun fetchSpecs(
        preferredName: String,
        preferredModel: String,
        vararg queries: String
    ): DeviceSpecs? {
        for (query in queries) {
            val specs = tryFetchSpecs(query, preferredName, preferredModel)
            if (specs != null) return specs
        }
        return null
    }

    private fun tryFetchSpecs(
        query: String,
        preferredName: String,
        preferredModel: String
    ): DeviceSpecs? {
        return try {
            val formattedQuery = query.replace(" ", "+")
            val searchUrl = "$BASE_URL/results.php3?sQuickSearch=yes&sName=$formattedQuery"

            var searchDoc: Document = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(30000)
                .get()

            var results = searchDoc.select(".makers li")
            
            // Fallback for model numbers (SM-G990B etc) which often don't show up in quick search
            if (results.isEmpty()) {
                val freeSearchUrl = "$BASE_URL/results.php3?sFreeSearch=yes&sFreeText=$formattedQuery"
                searchDoc = Jsoup.connect(freeSearchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(30000)
                    .get()
                results = searchDoc.select(".makers li")
            }
            
            if (results.isEmpty()) return null

            val bestMatchingElement = results.firstOrNull { element ->
                val deviceName = element.select("span").text()
                isBetterMatch(deviceName, preferredName, preferredModel)
            } ?: results.first()

            val devicePath = bestMatchingElement?.select("a")?.attr("href") ?: ""
            val searchThumbnail = bestMatchingElement?.select("img")?.attr("src") ?: ""
            
            if (devicePath.isBlank()) return null

            val deviceUrl =
                if (devicePath.startsWith("/")) "$BASE_URL$devicePath" else "$BASE_URL/$devicePath"

            val deviceDoc: Document = Jsoup.connect(deviceUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(30000)
                .get()

            val name = deviceDoc.select(".specs-phone-name-title").text()
            val tables = deviceDoc.select("table")
            val detailSpecs = mutableListOf<DeviceSpecCategory>()

            // Scrape images
            val imageUrls = mutableListOf<String>()

            // Fix protocol helper
            fun String.fixUrl(): String {
                return when {
                    startsWith("//") -> "https:$this"
                    startsWith("/") -> "$BASE_URL$this"
                    else -> this
                }
            }

            // Fallback to search thumbnail
            if (searchThumbnail.isNotBlank()) {
                imageUrls.add(searchThumbnail.fixUrl())
            }

            // Get main image on device page (often higher quality)
            deviceDoc.select(".specs-photo-main a img").firstOrNull()?.attr("src")?.let {
                val url = it.fixUrl()
                if (!imageUrls.contains(url)) imageUrls.add(0, url)
            }

            // Get more images from gallery if available
            val picturesLink =
                deviceDoc.select(".specs-links a:contains(Pictures)").firstOrNull()?.attr("href")
            if (picturesLink != null) {
                val picturesUrl = picturesLink.fixUrl()
                val picturesDoc = Jsoup.connect(picturesUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(30000)
                    .get()

                picturesDoc.select("#pictures-list img").forEach { img ->
                    val src = img.attr("src").ifBlank { img.attr("data-src") }
                    if (src.isNotBlank()) {
                        val url = src.fixUrl()
                        if (!imageUrls.contains(url)) {
                            imageUrls.add(url)
                        }
                    }
                }
            }

            tables.forEach { table ->
                val categoryName = table.select("th").firstOrNull()?.text() ?: ""
                val rows = table.select("tr")
                val specs = mutableListOf<DeviceSpecItem>()

                rows.forEach { row ->
                    val label = row.select("td.ttl").text()
                    val value = row.select("td.nfo").text()
                    if (label.isNotBlank() && value.isNotBlank()) {
                        specs.add(DeviceSpecItem(label, value))
                    }
                }

                if (categoryName.isNotBlank() && specs.isNotEmpty()) {
                    detailSpecs.add(DeviceSpecCategory(categoryName, specs))
                }
            }

            DeviceSpecs(name, detailSpecs, imageUrls)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isBetterMatch(
        foundName: String,
        preferredName: String,
        preferredModel: String
    ): Boolean {
        val found = foundName.lowercase()
        val prefName = preferredName.lowercase()
        val prefModel = preferredModel.lowercase()

        val variants = listOf("pro", "max", "plus", "xl", "ultra", "fold", "flip", "power", "neo", "gt", "lite", "ace", "prime", "edge", "fe")
        for (variant in variants) {
            if (found.contains(variant) && !prefName.contains(variant) && !prefModel.contains(variant)) {
                return false
            }
        }

        if (found.contains(prefName) || found.contains(prefModel)) {
            val modelIndex = found.indexOf(prefName).takeIf { it != -1 } ?: found.indexOf(prefModel)
            if (modelIndex != -1) {
                val afterModel = found.substring(modelIndex + (if (found.contains(prefName)) prefName.length else prefModel.length)).trim()
                if (afterModel.isNotEmpty()) {
                    val firstWord = afterModel.split(" ").firstOrNull() ?: ""
                    if (variants.contains(firstWord)) return false
                }
            }
            return true
        }

        return false
    }
}

package com.sameerasw.island.sdk

import android.graphics.Bitmap
import android.os.Bundle
import com.google.gson.Gson

class DecodedPayload(val payload: IslandPayload, val assets: Map<String, Bitmap>)

object IslandCodec {
    private val gson = Gson()

    fun encode(payload: IslandPayload, assets: Map<String, Bitmap> = emptyMap()): Triple<Int, String, Bundle> {
        val bundle = Bundle().apply { assets.forEach { (id, bitmap) -> putParcelable(id, bitmap) } }
        return Triple(payload.schema, gson.toJson(payload), bundle)
    }

    fun decode(schema: Int, json: String, assets: Bundle?): DecodedPayload? {
        if (schema > IslandSchema.VERSION) return null
        val payload = try {
            gson.fromJson(json, IslandPayload::class.java)
        } catch (_: Exception) {
            null
        } ?: return null
        val bitmaps = HashMap<String, Bitmap>()
        assets?.let { bundle ->
            bundle.classLoader = Bitmap::class.java.classLoader
            bundle.keySet().forEach { id ->
                @Suppress("DEPRECATION")
                (bundle.get(id) as? Bitmap)?.let { bitmaps[id] = it }
            }
        }
        return DecodedPayload(payload, bitmaps)
    }
}

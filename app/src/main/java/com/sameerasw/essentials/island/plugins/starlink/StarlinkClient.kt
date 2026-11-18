package com.sameerasw.essentials.island.plugins.starlink

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

/**
 * Talks to the dish's unauthenticated local gRPC-web endpoint. No Android or app dependencies.
 * Field numbers follow the community copy of the dish protos and can shift between firmware versions.
 */
object StarlinkClient {
    const val DEFAULT_HOST = "192.168.100.1"
    private const val GRPC_WEB_PORT = 9201
    private const val PATH = "/SpaceX.API.Device.Device/Handle"

    // Request { get_status = 1004 {} } wrapped in a gRPC-web data frame
    private val GET_STATUS_FRAME = byteArrayOf(0, 0, 0, 0, 3, 0xE2.toByte(), 0x3E, 0x00)

    fun fetchStatus(
        host: String = DEFAULT_HOST,
        connectTimeoutMs: Int = 1200,
        readTimeoutMs: Int = 2500,
        connect: (URL) -> URLConnection = { it.openConnection() },
    ): StarlinkStatus? {
        val connection = try {
            connect(URL("http://$host:$GRPC_WEB_PORT$PATH")) as HttpURLConnection
        } catch (_: Exception) {
            return null
        }
        return try {
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                useCaches = false
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty("Content-Type", "application/grpc-web+proto")
                setRequestProperty("Accept", "application/grpc-web+proto")
                setRequestProperty("X-Grpc-Web", "1")
            }
            connection.outputStream.use { it.write(GET_STATUS_FRAME) }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = connection.inputStream.use { stream ->
                ByteArrayOutputStream().also { stream.copyTo(it) }.toByteArray()
            }
            parseResponse(body)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseResponse(body: ByteArray): StarlinkStatus? {
        var offset = 0
        while (offset + 5 <= body.size) {
            val flag = body[offset].toInt() and 0xFF
            val length = readInt32(body, offset + 1)
            val start = offset + 5
            if (length < 0 || start + length > body.size) return null
            if (flag and 0x80 == 0) {
                val status = Proto.fields(body, start, start + length)
                    .firstOrNull { it.number == 2004 && it.bytes != null }
                    ?.let { parseStatus(it.bytes!!) }
                if (status != null) return status
            }
            offset = start + length
        }
        return null
    }

    private fun parseStatus(bytes: ByteArray): StarlinkStatus {
        var status = StarlinkStatus()
        for (field in Proto.fields(bytes, 0, bytes.size)) {
            status = when (field.number) {
                1 -> field.bytes?.let { info ->
                    var hardware: String? = null
                    var software: String? = null
                    Proto.fields(info, 0, info.size).forEach {
                        when (it.number) {
                            2 -> hardware = it.bytes?.decodeToString()
                            3 -> software = it.bytes?.decodeToString()
                        }
                    }
                    status.copy(hardwareVersion = hardware, softwareVersion = software)
                } ?: status
                2 -> field.bytes?.let { state ->
                    status.copy(uptimeS = Proto.fields(state, 0, state.size).firstOrNull { it.number == 1 }?.varint)
                } ?: status
                1006 -> status.copy(
                    state = when (field.varint?.toInt()) {
                        1 -> StarlinkState.Connected
                        2 -> StarlinkState.Searching
                        3 -> StarlinkState.Booting
                        else -> StarlinkState.Unknown
                    },
                )
                1005 -> field.bytes?.let { status.copy(alerts = parseAlerts(it)) } ?: status
                1003 -> status.copy(dropRate = field.float)
                1007 -> status.copy(downlinkBps = field.float)
                1008 -> status.copy(uplinkBps = field.float)
                1009 -> status.copy(pingMs = field.float)
                1004 -> field.bytes?.let { stats ->
                    var obstructed = false
                    var fraction: Float? = null
                    Proto.fields(stats, 0, stats.size).forEach {
                        when (it.number) {
                            5 -> obstructed = it.varint == 1L
                            1 -> fraction = it.float
                        }
                    }
                    status.copy(currentlyObstructed = obstructed, obstructionFraction = fraction)
                } ?: status
                else -> status
            }
        }
        return status
    }

    private fun parseAlerts(bytes: ByteArray): Set<StarlinkAlert> {
        val alerts = mutableSetOf<StarlinkAlert>()
        Proto.fields(bytes, 0, bytes.size).forEach {
            if (it.varint != 1L) return@forEach
            when (it.number) {
                1 -> alerts += StarlinkAlert.MotorsStuck
                2 -> alerts += StarlinkAlert.ThermalShutdown
                3 -> alerts += StarlinkAlert.ThermalThrottle
                4 -> alerts += StarlinkAlert.UnexpectedLocation
                5 -> alerts += StarlinkAlert.MastNotNearVertical
                6 -> alerts += StarlinkAlert.SlowEthernet
            }
        }
        return alerts
    }

    private fun readInt32(bytes: ByteArray, at: Int): Int =
        ((bytes[at].toInt() and 0xFF) shl 24) or ((bytes[at + 1].toInt() and 0xFF) shl 16) or
            ((bytes[at + 2].toInt() and 0xFF) shl 8) or (bytes[at + 3].toInt() and 0xFF)
}

internal object Proto {
    class Field(val number: Int, val varint: Long? = null, val bytes: ByteArray? = null, val fixed32: Int? = null) {
        val float: Float? get() = fixed32?.let { java.lang.Float.intBitsToFloat(it) }
    }

    fun fields(data: ByteArray, from: Int, to: Int): List<Field> {
        val out = ArrayList<Field>()
        var pos = from
        fun varint(): Long {
            var result = 0L
            var shift = 0
            while (pos < to) {
                val b = data[pos++].toInt()
                result = result or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return result
                shift += 7
                if (shift > 63) break
            }
            throw IllegalStateException("bad varint")
        }
        try {
            while (pos < to) {
                val tag = varint()
                val number = (tag ushr 3).toInt()
                when ((tag and 7).toInt()) {
                    0 -> out += Field(number, varint = varint())
                    1 -> pos += 8
                    2 -> {
                        val len = varint().toInt()
                        if (len < 0 || pos + len > to) return out
                        out += Field(number, bytes = data.copyOfRange(pos, pos + len))
                        pos += len
                    }
                    5 -> {
                        if (pos + 4 > to) return out
                        val v = (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8) or
                            ((data[pos + 2].toInt() and 0xFF) shl 16) or ((data[pos + 3].toInt() and 0xFF) shl 24)
                        out += Field(number, fixed32 = v)
                        pos += 4
                    }
                    else -> return out
                }
            }
        } catch (_: IllegalStateException) {
        }
        return out
    }
}

package com.rc.playgrounds.server

import java.io.ByteArrayOutputStream

internal object H264Packets {
    const val MAX_PACKET_BYTES = 1200
    const val NAL_IDR = 5
    const val NAL_SPS = 7

    private val START_CODE = byteArrayOf(0, 0, 0, 1)

    fun toAnnexB(data: ByteArray): ByteArray {
        if (startCodeLength(data) > 0) {
            return data
        }
        val out = ByteArrayOutputStream(data.size + START_CODE.size * 4)
        var offset = 0
        var nals = 0
        while (offset + 4 <= data.size) {
            val length = ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)
            val end = offset + 4 + length
            if (length <= 0 || end > data.size) {
                break
            }
            out.write(START_CODE)
            out.write(data, offset + 4, length)
            offset = end
            nals++
        }
        if (nals == 0) {
            return START_CODE + data
        }
        return out.toByteArray()
    }

    fun packets(annexB: ByteArray, maxPacketBytes: Int = MAX_PACKET_BYTES): List<ByteArray> {
        if (annexB.isEmpty()) {
            return emptyList()
        }
        val chunks = ArrayList<ByteArray>((annexB.size + maxPacketBytes - 1) / maxPacketBytes)
        var offset = 0
        while (offset < annexB.size) {
            val end = minOf(offset + maxPacketBytes, annexB.size)
            chunks.add(annexB.copyOfRange(offset, end))
            offset = end
        }
        return chunks
    }

    fun nalType(data: ByteArray): Int {
        val offset = startCodeLength(data)
        if (offset <= 0 || offset >= data.size) {
            return -1
        }
        return data[offset].toInt() and 0x1F
    }

    private fun startCodeLength(data: ByteArray): Int {
        if (data.size >= 4 &&
            data[0] == 0.toByte() &&
            data[1] == 0.toByte() &&
            data[2] == 0.toByte() &&
            data[3] == 1.toByte()
        ) {
            return 4
        }
        if (data.size >= 3 &&
            data[0] == 0.toByte() &&
            data[1] == 0.toByte() &&
            data[2] == 1.toByte()
        ) {
            return 3
        }
        return 0
    }
}

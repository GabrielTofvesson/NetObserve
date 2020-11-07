package dev.w1zzrd.packets.link

import dev.w1zzrd.packets.Packet
import java.nio.ByteOrder

class EthernetPacket: Packet {
    constructor(
            data: ByteArray,
            offset: UInt,
            contentLength: UInt,
            byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
    ) : super(data, offset, contentLength, 14u, byteOrder)
    constructor(
            wrap: Packet,
            byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
    ) : super(wrap, 14u, byteOrder)

    val destination by lazy { MACAddress(*sequenceAt(0u, 6u)) }
    val source by lazy { MACAddress(*sequenceAt(6u, 12u)) }
    val type: UShort
        get() = uShortAt(12u)
}
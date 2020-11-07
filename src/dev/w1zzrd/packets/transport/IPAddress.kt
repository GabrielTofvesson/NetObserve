package dev.w1zzrd.packets.transport

import java.nio.ByteBuffer

sealed class IPAddress(val addr: ByteArray, bytes: UInt) {
    class IPv4Address(addr: ByteArray) : IPAddress(addr, 4u) {
        override fun toString() = "${addr[0]}.${addr[1]}.${addr[2]}.${addr[3]}"
    }

    class IPv6Address(addr: ByteArray) : IPAddress(addr, 16u) {
        override fun toString(): String {
            val bb = ByteBuffer.wrap(addr)
            fun addrPart(idx: Int) =
                    if (bb.getShort(idx shl 1) == 0.toShort()) ""
                    else bb.getShort(idx shl 1).toString(16)

            return "${addrPart(0)}:${addrPart(1)}:${addrPart(2)}:${addrPart(3)}:${addrPart(4)}:${addrPart(5)}:${addrPart(6)}:${addrPart(7)}"
        }
    }

    init {
        if (addr.size != bytes.toInt())
            throw IllegalArgumentException("Not a valid address")
    }

    abstract override fun toString(): String
}
package dev.w1zzrd.packets.link

class MACAddress(vararg addr: Byte) {
    init {
        if (addr.size != 6)
            throw IllegalArgumentException("Not a MAC address")
    }
}
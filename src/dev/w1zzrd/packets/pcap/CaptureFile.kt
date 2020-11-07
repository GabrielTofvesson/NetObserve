package dev.w1zzrd.packets.pcap

import dev.w1zzrd.extensions.readInt
import dev.w1zzrd.extensions.readUInt
import dev.w1zzrd.extensions.readUShort
import dev.w1zzrd.packets.Packet
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CaptureFile(file: File, private val packets: ArrayList<Packet> = ArrayList()): List<Packet> by packets {
    val byteOrder: ByteOrder

    val h_magic_number: UInt
    val h_version_major: UShort
    val h_version_minor: UShort
    val h_thiszone: Int
    val h_sigfigs: UInt
    val h_snaplen: UInt
    val h_network: UInt

    val packetCount: Int
        get() = packets.size

    init {
        // Check that the file exists
        if (!file.isFile)
            throw FileNotFoundException()

        // Check that the PCAP global header exists
        if (file.length() < 24)
            throw IllegalArgumentException("File is not long enough")

        // Read PCAP global header
        val buf = ByteArray(4)
        val bufWrap = ByteBuffer.wrap(buf)
        val istream = file.inputStream()

        h_magic_number = istream.readUInt(bufWrap)

        byteOrder = when (h_magic_number) {
            0xa1b2c3d4u -> ByteOrder.BIG_ENDIAN
            0xd4c3b2a1u -> ByteOrder.LITTLE_ENDIAN
            else -> throw RuntimeException("Bad file magic")
        }

        bufWrap.order(byteOrder)

        h_version_major = istream.readUShort(bufWrap)
        h_version_minor = istream.readUShort(bufWrap)
        h_thiszone = istream.readInt(bufWrap)
        h_sigfigs = istream.readUInt(bufWrap)
        h_snaplen = istream.readUInt(bufWrap)
        h_network = istream.readUInt(bufWrap)

        // Read packets
        /*
        var readCount = 24
        while (readCount < file.length()) {
            val packet = Packet(istream, byteOrder)

            readCount += 32 + packet.packetData.size
            packets.add(packet)
        }
        */
    }
}
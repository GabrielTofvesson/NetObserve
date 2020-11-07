package dev.w1zzrd.packets.transport

import dev.w1zzrd.packets.Packet
import dev.w1zzrd.packets.transport.DSCPType.Companion.getDSCPType
import dev.w1zzrd.packets.transport.DatagramProtocol.Companion.toDatagramProtocol
import dev.w1zzrd.packets.transport.IP4OptionType.Companion.toIP4OptionType
import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.reflect.full.functions

inline fun <reified T> UInt.toEnum(): T where T: Enum<T> {
    val values = (T::class.functions.first { it.name == "values" }.call() as Array<T>)

    if (this < values.size.toUInt()) return values[this.toInt()]
    else throw IllegalArgumentException("Index out of bounds")
}

enum class ToSPrecedenceType(val literal: UInt) {
    ROUTINE(0b000u),
    PRIORITY(0b001u),
    IMMEDIATE(0b010u),
    FLASH(0b011u),
    FLASH_OVERRIDE(0b100u),
    ECP(0b101u),
    INTERNET_CONTROL(0b110u),
    NETWORK_CONTROL(0b111u)
}

enum class DSCPGrade {
    NONE, GOLD, SILVER, BRONZE
}

enum class DSCPType {
    BEST_EFFORT, CLASSED, EXPRESS_FORWARDING, EXPEDITED_FORWARDING, CONTROL;

    companion object {
        fun UByte.getDSCPType() =
                when(this.toUInt() and 0x3Fu) {
                    0u -> BEST_EFFORT
                    in 8u until 40u -> CLASSED
                    in 40u until 46u -> EXPRESS_FORWARDING
                    in 46u until 48u -> EXPEDITED_FORWARDING
                    in 48u until 63u -> CONTROL
                    else -> throw IllegalStateException("This is impossible to reach")
                }
    }
}

enum class IP4OptionType {
    CONTROL, DEBUG_MEASURE, RESERVED;

    companion object {
        fun UByte.toIP4OptionType() =
                when (this.toUInt()) {
                    0u -> CONTROL
                    2u -> DEBUG_MEASURE
                    1u, 3u -> RESERVED
                    else -> throw IllegalArgumentException("Out of acceptable range")
                }
    }
}

class IP4Packet: Packet {
    companion object {
        private fun readHeaderLength(data: ByteArray, offset: UInt) = ((data[offset.toInt()] and 0xF).toInt() shl 2).toUInt()
    }

    inner class TypeOfService {
        val rawPrecedence by BitsAt(1u, 5u, 3u)
        val precedence get() = rawPrecedence.toUInt().toEnum<ToSPrecedenceType>()
        val delay by BitsAt(1u, 4u)
        val throughput by BitsAt(1u, 3u)
        val reliability by BitsAt(1u, 2u)
        val cost by BitsAt(1u, 1u)
        val MBZ by BitsAt(1u, 0u)
    }


    open inner class DifferentiatedServicesCodePoint internal constructor() {
        val rawType by BitsAt(1u, 2u, 6u)
        val type get() = rawType.getDSCPType()
    }
    inner class GradedService : DifferentiatedServicesCodePoint() {
        val serviceClass by BitsAt(1u, 5u, 3u)

        val rawServiceGrade by BitsAt(1u, 3u, 2u)
        val serviceGrade get() = rawServiceGrade.toUInt().toEnum<DSCPGrade>()
    }

    inner class IP4Flags {
        val reserved by BitsAt(6u, 7u)
        val dontFragment by BitsAt(6u, 6u)
        val moreFragments by BitsAt(6u, 5u)
        val fragmentOffset by BitsAt(6u, 0u, 5u)
    }

    // IPv4 options field parser
    inner class IP4Options(private var optionIndices: UIntArray = UIntArray(0)): Collection<UInt> by optionIndices {
        init {
            if (headerLength == 20u) optionIndices = UIntArray(0)
            else {
                var readCount = 0u
                var total = 0u

                val offsets = ArrayList<UInt>()

                do {
                    ++total
                    offsets.add(readCount)

                    val option = IP4Option(readCount)

                    readCount += option.optionSize
                } while (readCount < headerLength && option.optionNumber != 0u.toUByte())

                optionIndices = offsets.toUIntArray()
            }
        }

        operator fun get(index: Int): IP4Option {
            if (index >= optionIndices.size || index < 0)
                throw IndexOutOfBoundsException("Getting IPv4 option out of bounds")

            return IP4Option(optionIndices[index])
        }
    }

    // IPv4 option field
    inner class IP4Option(offset: UInt) {
        // EOOL and NOP do not declare a length
        val isSimpleOption
            get() = optionNumber == 0u.toUByte() || optionNumber == 1u.toUByte()
        val copied by BitsAt(offset, 7u)
        val rawOptionClass by BitsAt(offset, 5u, 2u)
        val optionClass
            get() = rawOptionClass.toIP4OptionType()
        val optionNumber by BitsAt(offset, 0u, 5u)
        val optionSize
            get() = if(isSimpleOption) 1u else byteAt(offset + 1u).toUInt()
        val optionData
            get() = sequenceAt(offset + 2u, optionSize - 2u)
    }



    constructor(data: ByteArray, offset: UInt, contentLength: UInt, byteOrder: ByteOrder) :
            super(data, offset, contentLength, readHeaderLength(data, offset), byteOrder)
    constructor(wrap: Packet, byteOrder: ByteOrder) :
            super(wrap, readHeaderLength(wrap.data, wrap.offset + wrap.headerSize), byteOrder)



    // IPv4 header fields
            val version         by      BitsAt(0u, 4u, 4u)
            val headerLength    get() = bitsAt(0u, 0u, 4u) shl 2
            val typeOfService =         TypeOfService()
    private val ECNBits         by      BitsAt(1u, 0u, 2u)
            val ECN             get() = ECNBits.toUInt().toEnum<ExplicitCongestionNotification>()
    private val DSCPBits        by      BitsAt(1u, 2u, 6u)
            val DSCP            get() = DSCPBits.toDSCP()
            val reportedLength  by      ShortAt(2u)
            val id              by      ShortAt(5u)
            val fragments =             IP4Flags()
            val TTL             by      UByteAt(7u)
    private val protocolByte    by      UByteAt(8u)
            val protocol        get() = protocolByte.toDatagramProtocol()
            val checksum        by      UShortAt(10u)
            val source          get() = IPAddress.IPv4Address(sequenceAt(12u, 4u))
            val destination     get() = IPAddress.IPv4Address(sequenceAt(16u, 4u))
            val options =               IP4Options()



    private fun UByte.toDSCP() =
            when(getDSCPType()) {
                DSCPType.CLASSED -> GradedService()
                else -> DifferentiatedServicesCodePoint()
            }
}
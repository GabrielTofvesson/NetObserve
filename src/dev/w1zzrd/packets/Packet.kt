package dev.w1zzrd.packets

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.KProperty

/**
 * Packet structure:
 *
 *      Byte Array:
 *
 *          [...,*,*,*,*,H,H,H,H,D,D,D,*,*,*,*,...]
 *
 *      *: Not packet
 *
 *      H: Packet Header
 *
 *      D: Packet data
 *
 *      Data before first 'H' is the offset.
 *
 *      Length between first 'H' and last 'D' is length.
 *
 *      Length between first 'H' and last 'H' is headerLength.
 */
open class Packet(
        val data: ByteArray,
        val offset: UInt,
        val contentLength: UInt,
        val headerSize: UInt,
        val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
) {
    constructor(wrap: Packet, headerLength: UInt, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN) :
            this(
                    wrap.data,
                    wrap.offset + wrap.headerSize,
                    if (headerLength > wrap.contentLength)
                        throw IllegalArgumentException("Length of header exceeds content bounds")
                    else wrap.contentLength - headerLength,
                    headerLength,
                    byteOrder
            )

    init {
        if (data.size.toUInt() < offset + headerSize + contentLength)
            throw IllegalArgumentException("Packet exceeds array bounds!")
    }


    protected fun offsetAt(offset: UInt) = (this.offset + offset).toInt()

    protected abstract class InnerProvidable<T, R> where T: Any {
        private var _thisRef: T? = null
        protected val thisRef
            get() = _thisRef!!

        operator fun getValue(thisRef: Any?, property: KProperty<*>) = getValue(property)
        abstract fun getValue(property: KProperty<*>): R


        // This is a hack to provide delegates referencing fields in outer classes
        operator fun provideDelegate(thisRef: Any, property: KProperty<*>): InnerProvidable<T, R> {
            val outer = thisRef.javaClass.declaredFields.firstOrNull { it.name == "this$0" }
            outer?.isAccessible = true

            _thisRef =
                    if (outer != null) outer.get(thisRef) as T
                    else thisRef as T
            return this
        }
    }
    protected open class ValueAt<T>(private val byteOffset: UInt, private val toValue: ByteBuffer.(Int) -> T): InnerProvidable<Packet, T>() {
        override fun getValue(property: KProperty<*>) =
                ByteBuffer
                        .wrap(thisRef.data, thisRef.offset.toInt(), thisRef.data.size - thisRef.offset.toInt())
                        .order(thisRef.byteOrder)
                        .toValue(byteOffset.toInt())
    }

    protected class ByteAt(byteOffset: UInt) : ValueAt<Byte>(byteOffset, ByteBuffer::get)
    protected class ShortAt(byteOffset: UInt) : ValueAt<Short>(byteOffset, ByteBuffer::getShort)
    protected class IntAt(byteOffset: UInt) : ValueAt<Int>(byteOffset, ByteBuffer::getInt)
    protected class LongAt(byteOffset: UInt) : ValueAt<Long>(byteOffset, ByteBuffer::getLong)
    protected class FloatAt(byteOffset: UInt) : ValueAt<Float>(byteOffset, ByteBuffer::getFloat)
    protected class DoubleAt(byteOffset: UInt) : ValueAt<Double>(byteOffset, ByteBuffer::getDouble)

    protected class UByteAt(byteOffset: UInt) : ValueAt<UByte>(byteOffset, { get(it).toUByte() })
    protected class UShortAt(byteOffset: UInt) : ValueAt<UShort>(byteOffset, { getShort(it).toUShort() })
    protected class UIntAt(byteOffset: UInt) : ValueAt<UInt>(byteOffset, { getInt(it).toUInt() })
    protected class ULongAt(byteOffset: UInt) : ValueAt<ULong>(byteOffset, { getLong(it).toULong() })

    protected class BitsAt(private val byteOffset: UInt, private val bitOffset: UInt, private val count: UInt = 1u): InnerProvidable<Packet, UByte>() {
        init {
            // This is a limitation for performance reasons
            if (count + bitOffset > 8u)
                throw IndexOutOfBoundsException("Cannot index bits across byte boundary")
        }

        override fun getValue(property: KProperty<*>) =
                (thisRef.data[thisRef.offsetAt(byteOffset)].toUInt() and 0xFFu shr bitOffset.toInt() and (0xFFu shr (8 - count.toInt()))).toUByte()
    }

    protected class SequenceAt(private val byteOffset: UInt, private val length: UInt): InnerProvidable<Packet, ByteArray>() {
        override fun getValue(property: KProperty<*>) =
                thisRef.data.slice(thisRef.offsetAt(byteOffset) until thisRef.offsetAt(byteOffset) + length.toInt()).toByteArray()
    }




    protected fun bitAt(byteOffset: UInt, bitOffset: UInt) =
            if (bitOffset > 7u) throw RuntimeException("Bit offset cannot exceed 7")
            else (data[offsetAt(byteOffset)].toInt() shr bitOffset.toInt() and 1).toUInt()

    protected fun bitsAt(byteOffset: UInt, bitOffset: UInt, seqLength: UInt) =
            when {
                bitOffset > 7u -> throw RuntimeException("Bit offset cannot exceed 7")
                seqLength > 8u - bitOffset -> throw RuntimeException("Sequence length cannot exceed byte limit")
                seqLength == 0u -> throw RuntimeException("Sequence length cannot be zero")
                else -> (data[offsetAt(byteOffset)].toInt() shr bitOffset.toInt() and (0xFF ushr (8u - seqLength).toInt())).toUInt()
            }

    protected fun byteAt(offset: UInt) = data[offsetAt(offset)]
    protected fun shortAt(offset: UInt) = ByteBuffer.wrap(data).order(this.byteOrder).getShort(offsetAt(offset))
    protected fun intAt(offset: UInt) = ByteBuffer.wrap(data).order(this.byteOrder).getInt(offsetAt(offset))
    protected fun longAt(offset: UInt) = ByteBuffer.wrap(data).order(this.byteOrder).getLong(offsetAt(offset))

    protected fun uByteAt(offset: UInt) = byteAt(offset).toUByte()
    protected fun uShortAt(offset: UInt) = shortAt(offset).toUShort()
    protected fun uIntAt(offset: UInt) = intAt(offset).toUInt()
    protected fun uLongAt(offset: UInt) = longAt(offset).toULong()

    protected fun floatAt(offset: UInt) = ByteBuffer.wrap(data).order(this.byteOrder).getFloat(offsetAt(offset))
    protected fun doubleAt(offset: UInt) = ByteBuffer.wrap(data).order(this.byteOrder).getDouble(offsetAt(offset))

    protected fun sequenceAt(offset: UInt, length: UInt) = data.slice(offsetAt(offset) until length.toInt()).toByteArray()
}
package dev.w1zzrd.extensions

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun InputStream.readShort(byteBuffer: ByteBuffer = ByteBuffer.allocate(2)): Short {
    read(byteBuffer.array(), 0, 2)
    return byteBuffer.getShort(0)
}

fun InputStream.readInt(byteBuffer: ByteBuffer = ByteBuffer.allocate(4)): Int {
    read(byteBuffer.array(), 0, 4)
    return byteBuffer.getInt(0)
}

fun InputStream.readLong(byteBuffer: ByteBuffer = ByteBuffer.allocate(8)): Long {
    read(byteBuffer.array(), 0, 8)
    return byteBuffer.getLong(0)
}

fun InputStream.readUShort(byteBuffer: ByteBuffer = ByteBuffer.allocate(2)) =
        readShort(byteBuffer).toUShort()

fun InputStream.readUInt(byteBuffer: ByteBuffer = ByteBuffer.allocate(4)) =
        readInt(byteBuffer).toUInt()

fun InputStream.readULong(byteBuffer: ByteBuffer = ByteBuffer.allocate(8)) =
        readLong(byteBuffer).toULong()

fun InputStream.readFloat(byteBuffer: ByteBuffer = ByteBuffer.allocate(4)): Float {
    read(byteBuffer.array(), 0, 4)
    return byteBuffer.getFloat(0)
}

fun InputStream.readDouble(byteBuffer: ByteBuffer = ByteBuffer.allocate(8)): Double {
    read(byteBuffer.array(), 0, 8)
    return byteBuffer.getDouble(0)
}


fun InputStream.readShort(byteOrder: ByteOrder) = readShort(ByteBuffer.allocate(2).order(byteOrder))
fun InputStream.readInt(byteOrder: ByteOrder) = readInt(ByteBuffer.allocate(4).order(byteOrder))
fun InputStream.readLong(byteOrder: ByteOrder) = readLong(ByteBuffer.allocate(8).order(byteOrder))
fun InputStream.readUShort(byteOrder: ByteOrder) = readUShort(ByteBuffer.allocate(2).order(byteOrder))
fun InputStream.readUInt(byteOrder: ByteOrder) = readUInt(ByteBuffer.allocate(4).order(byteOrder))
fun InputStream.readULong(byteOrder: ByteOrder) = readULong(ByteBuffer.allocate(8).order(byteOrder))
fun InputStream.readFloat(byteOrder: ByteOrder) = readFloat(ByteBuffer.allocate(4).order(byteOrder))
fun InputStream.readDouble(byteOrder: ByteOrder) = readDouble(ByteBuffer.allocate(8).order(byteOrder))
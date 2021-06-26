package com.polidea.rxandroidble2.samplekotlin.util

import java.lang.StringBuilder
import kotlin.experimental.and

fun ByteArray.toHex() = joinToString("") { String.format("%02X", (it.toInt() and 0xff)) }

private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()

fun ByteArray.bytesToHex(bytes: ByteArray): String {
    val hexChars = CharArray(bytes.size * 2)
    for (j in bytes.indices) {
        val v: Int = (bytes[j] and 0xFF.toByte()).toInt()
        hexChars[j * 2] = HEX_ARRAY.get(v ushr 4)
        hexChars[j * 2 + 1] = HEX_ARRAY.get(v and 0x0F)
    }
    return String(hexChars)
}

fun ByteArray.toHexString(): String {
    return String(this ,charset("UTF-8"))
}

fun String.toDoubleByte(): String{
    val hex = StringBuilder()
    hex.append(this.toInt().toString(16))
    var len = hex.length
    while (len < 4){
        hex.insert(0, "0")
        len++
    }
    return hex.toString().toUpperCase()
}

fun ByteArray.hex2byte(): ByteArray {
    var b = this
    require(b!!.size % 2 == 0) { "长度不是偶数" }
    val b2 = ByteArray(b.size / 2)
    var n = 0
    while (n < b.size) {
        val item = String(b, n, 2)
        // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个进制字节
        b2[n / 2] = item.toInt(16).toByte()
        n += 2
    }
    //b = null
    return b2
}
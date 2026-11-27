package com.flixvora.data

/**
 * Compares dotted version names ("11.4", "11.10.2") numerically.
 * Kept pure so it can be unit tested without Android.
 */
object AppVersion {

    fun isNewer(remote: String, current: String): Boolean {
        val a = remote.trim().removePrefix("v").split('.')
        val b = current.trim().removePrefix("v").split('.')
        val size = maxOf(a.size, b.size)
        for (i in 0 until size) {
            val x = a.getOrNull(i)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            val y = b.getOrNull(i)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            if (x != y) return x > y
        }
        return false
    }
}

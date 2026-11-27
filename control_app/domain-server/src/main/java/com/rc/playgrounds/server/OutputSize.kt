package com.rc.playgrounds.server

internal fun outputSize(
    srcWidth: Int,
    srcHeight: Int,
    rotation: Int,
    targetHeight: Int,
): Pair<Int, Int> {
    val (orientedWidth, orientedHeight) = orientedSize(srcWidth, srcHeight, rotation)
    val height = evenAtLeast16(targetHeight)
    val width = evenAtLeast16(
        (height.toLong() * orientedWidth / orientedHeight.coerceAtLeast(1)).toInt(),
    )
    return width to height
}

internal fun orientedSize(srcWidth: Int, srcHeight: Int, rotation: Int): Pair<Int, Int> {
    return if (swapsAxes(rotation)) srcHeight to srcWidth else srcWidth to srcHeight
}

private fun swapsAxes(rotation: Int): Boolean {
    val quarter = normalizedRotation(rotation)
    return quarter == 90 || quarter == 270
}

internal fun normalizedRotation(rotation: Int): Int {
    val mod = rotation % 360
    return if (mod < 0) mod + 360 else mod
}

private fun evenAtLeast16(value: Int): Int {
    val atLeast = value.coerceAtLeast(16)
    return atLeast - atLeast % 2
}

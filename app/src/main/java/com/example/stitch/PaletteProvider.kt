package com.example.stitch

interface PaletteProvider {
    val all: List<DmcColor>
    fun nearest(lab: FloatArray): DmcColor
    fun nearestIndex(lab: FloatArray, list: List<DmcColor>): Int
}

package com.example.subsinthe.crossline.util

import java.lang.System

class Lifespan(private val lifespan: Long) {
    private val start = System.currentTimeMillis()

    val valid: Boolean get() = (System.currentTimeMillis() - start) <= lifespan
}

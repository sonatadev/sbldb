package com.github.sonatadev.sbldb.data.entity

/** How a set was performed. Warm-ups never count toward volume; every other type is a working set. */
enum class SetType {
    NORMAL, WARMUP, DROP, MYO, PARTIALS, FAILURE;

    val countsTowardVolume: Boolean get() = this != WARMUP
}

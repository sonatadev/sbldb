package com.github.sonatadev.sbldb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OneRepMaxTest {
    @Test
    fun `epley estimate`() {
        assertEquals(120.0, OneRepMax.epley(100.0, 6)!!, 1e-9)
        assertEquals(100.0, OneRepMax.epley(100.0, 1)!!, 1e-9)
    }

    @Test
    fun `unsuitable sets give no estimate`() {
        assertNull(OneRepMax.epley(100.0, 13))
        assertNull(OneRepMax.epley(100.0, 0))
        assertNull(OneRepMax.epley(null, 5))
        assertNull(OneRepMax.epley(0.0, 5))
    }
}

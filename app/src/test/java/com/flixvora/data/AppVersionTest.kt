package com.flixvora.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun `compares numeric segments not strings`() {
        assertTrue(AppVersion.isNewer("11.10", "11.9"))
        assertFalse(AppVersion.isNewer("11.9", "11.10"))
        assertFalse(AppVersion.isNewer("11.5", "11.5"))
        assertTrue(AppVersion.isNewer("v12.0", "11.99.9"))
        assertTrue(AppVersion.isNewer("11.5.1", "11.5"))
        assertFalse(AppVersion.isNewer("11.5", "11.5.1"))
    }
}

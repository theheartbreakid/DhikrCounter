package com.Crescent.DhikrCounter.core.update

import org.junit.Assert.*
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun testParseReleaseVersion() {
        assertEquals(
            UpdateManager.Version(1, 0, 0),
            UpdateManager.parseReleaseVersion("1.0.0")
        )
        assertEquals(
            UpdateManager.Version(1, 0, 0),
            UpdateManager.parseReleaseVersion("v1.0.0")
        )
        assertEquals(
            UpdateManager.Version(1, 0, 0),
            UpdateManager.parseReleaseVersion("Version 1.0.0")
        )
        assertEquals(
            UpdateManager.Version(1, 0, 0),
            UpdateManager.parseReleaseVersion("Release 1.0.0")
        )
        assertEquals(
            UpdateManager.Version(1, 0, 0),
            UpdateManager.parseReleaseVersion("Dhikr Counter 1.0.0")
        )
        assertEquals(
            UpdateManager.Version(1, 0, 10),
            UpdateManager.parseReleaseVersion("1.0.10")
        )
        assertEquals(
            UpdateManager.Version(2, 1, 3),
            UpdateManager.parseReleaseVersion("v2.1.3")
        )

        // Invalid cases
        assertNull(UpdateManager.parseReleaseVersion(""))
        assertNull(UpdateManager.parseReleaseVersion("build-36006558804"))
        assertNull(UpdateManager.parseReleaseVersion("Production Build #47"))
        assertNull(UpdateManager.parseReleaseVersion("invalid"))
    }

    @Test
    fun testCompareVersions() {
        // 1.0.1 -> 1.0.2 UPDATE (remote > current => > 0)
        val v1_0_1 = UpdateManager.Version(1, 0, 1)
        val v1_0_2 = UpdateManager.Version(1, 0, 2)
        assertTrue(UpdateManager.compareVersions(v1_0_1, v1_0_2) > 0)

        // 1.0.9 -> 1.0.10 UPDATE
        val v1_0_9 = UpdateManager.Version(1, 0, 9)
        val v1_0_10 = UpdateManager.Version(1, 0, 10)
        assertTrue(UpdateManager.compareVersions(v1_0_9, v1_0_10) > 0)

        // 1.1.0 -> 1.0.9 NO UPDATE (remote < current => < 0)
        val v1_1_0 = UpdateManager.Version(1, 1, 0)
        assertTrue(UpdateManager.compareVersions(v1_1_0, v1_0_9) < 0)

        // 1.9.9 -> 2.0.0 UPDATE
        val v1_9_9 = UpdateManager.Version(1, 9, 9)
        val v2_0_0 = UpdateManager.Version(2, 0, 0)
        assertTrue(UpdateManager.compareVersions(v1_9_9, v2_0_0) > 0)

        // 2.0.0 -> 2.0.0 UP TO DATE (== 0)
        assertEquals(0, UpdateManager.compareVersions(v2_0_0, v2_0_0))
    }
}

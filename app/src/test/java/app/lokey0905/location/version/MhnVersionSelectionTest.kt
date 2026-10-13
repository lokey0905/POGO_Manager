package app.lokey0905.location.version

import org.junit.Assert.assertEquals
import org.junit.Test

class MhnVersionSelectionTest {
    @Test
    fun keepsSelectedOlderVersionWhileItRemainsAvailable() {
        assertEquals(
            "0.10.0",
            selectMhnTargetVersion(listOf("0.11.0", "0.10.0"), "0.10.0", "0.11.0")
        )
    }

    @Test
    fun missingOrInitialSelectionUsesTheFirstAvailableVersion() {
        val available = listOf("0.11.0", "0.10.0")
        assertEquals("0.11.0", selectMhnTargetVersion(available, "0.9.0", "0.12.0"))
        assertEquals("0.11.0", selectMhnTargetVersion(available, "未安裝", "0.12.0"))
    }

    @Test
    fun emptyListUsesTheExactExistingFallback() {
        assertEquals("0.11.0", selectMhnTargetVersion(emptyList(), "0.10.0", "0.11.0"))
        assertEquals("未安裝", selectMhnTargetVersion(emptyList(), "0.10.0", "未安裝"))
        assertEquals("", selectMhnTargetVersion(emptyList(), "0.10.0", ""))
    }

    @Test
    fun doesNotSortOrMutateTheSourceOrder() {
        val available = mutableListOf("0.9.0", "0.11.0", "0.10.0")
        val before = available.toList()

        assertEquals("0.9.0", selectMhnTargetVersion(available, "unknown", "0.11.0"))
        assertEquals(before, available)
    }
}

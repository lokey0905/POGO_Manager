package app.lokey0905.location.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabNavigationStateTest {
    @Test
    fun missingSavedStateStartsAtHomeWithoutBackHistory() {
        val state = TabNavigationState.restore(null)

        assertEquals(MainTab.HOME, state.current)
        assertNull(state.previous)
        assertFalse(state.canGoBack)
        assertEquals(listOf("HOME"), state.save())
    }

    @Test
    fun emptySavedStateUsesFallback() {
        val state = TabNavigationState.restore(emptyList(), MainTab.POKEMON)

        assertEquals(MainTab.POKEMON, state.current)
        assertNull(state.previous)
        assertFalse(state.canGoBack)
    }

    @Test
    fun entirelyUnknownSavedStateUsesFallback() {
        val state = TabNavigationState.restore(
            listOf("", "REMOVED_TAB", "home"), MainTab.SETTINGS
        )

        assertEquals(listOf("HOME", "SETTINGS"), state.save())
        assertTrue(state.canGoBack)
    }

    @Test
    fun everyDestinationCanBeTheFallback() {
        MainTab.entries.forEach { tab ->
            val state = TabNavigationState.restore(null, tab)

            assertEquals(tab, state.current)
            if (tab == MainTab.SETTINGS) {
                assertEquals(listOf("HOME", "SETTINGS"), state.save())
                assertTrue(state.goBack())
                assertEquals(MainTab.HOME, state.current)
            } else {
                assertEquals(listOf(tab.name), state.save())
                assertFalse(state.goBack())
            }
        }
    }

    @Test
    fun allFivePrimaryDestinationsAndSettingsAreSelectable() {
        val state = TabNavigationState.restore(null)
        val destinations = listOf(
            MainTab.POKEMON,
            MainTab.MONSTER_HUNTER,
            MainTab.HOME,
            MainTab.SHORTCUTS,
            MainTab.PIKMIN,
            MainTab.SETTINGS
        )

        destinations.forEach { tab ->
            assertTrue(state.select(tab))
            assertEquals(tab, state.current)
        }

        assertEquals(listOf("HOME") + destinations.map { it.name }, state.save())
    }

    @Test
    fun reselectingCurrentTabDoesNotAddBackHistory() {
        val state = TabNavigationState.restore(null)

        repeat(3) { assertFalse(state.select(MainTab.HOME)) }
        assertFalse(state.canGoBack)
        assertNull(state.previous)

        assertTrue(state.select(MainTab.POKEMON))
        repeat(3) { assertFalse(state.select(MainTab.POKEMON)) }
        assertEquals(listOf("HOME", "POKEMON"), state.save())
        assertEquals(MainTab.HOME, state.previous)
    }

    @Test
    fun selectingAPreviouslyVisitedTabPreservesChronologicalHistory() {
        val state = TabNavigationState.restore(null)

        state.select(MainTab.POKEMON)
        state.select(MainTab.PIKMIN)
        state.select(MainTab.POKEMON)

        assertEquals(listOf("HOME", "POKEMON", "PIKMIN", "POKEMON"), state.save())
        assertEquals(MainTab.PIKMIN, state.previous)
        assertTrue(state.goBack())
        assertEquals(MainTab.PIKMIN, state.current)
        assertEquals(MainTab.POKEMON, state.previous)
    }

    @Test
    fun backVisitsEveryEntryInReverseChronologicalOrderUntilRoot() {
        val visited = listOf(
            MainTab.HOME,
            MainTab.POKEMON,
            MainTab.MONSTER_HUNTER,
            MainTab.SHORTCUTS,
            MainTab.SETTINGS,
            MainTab.POKEMON
        )
        val state = TabNavigationState.restore(visited.map { it.name })

        for (index in visited.lastIndex downTo 1) {
            assertEquals(visited[index], state.current)
            assertEquals(visited[index - 1], state.previous)
            assertTrue(state.canGoBack)
            assertTrue(state.goBack())
            assertEquals(visited[index - 1], state.current)
        }

        assertEquals(MainTab.HOME, state.current)
        assertFalse(state.canGoBack)
        assertNull(state.previous)
        assertFalse(state.goBack())
    }

    @Test
    fun backAtRootIsANoOpEvenWhenRootIsNotHome() {
        val state = TabNavigationState.restore(listOf("SHORTCUTS"))

        repeat(3) { assertFalse(state.goBack()) }

        assertEquals(MainTab.SHORTCUTS, state.current)
        assertEquals(listOf("SHORTCUTS"), state.save())
        assertNull(state.previous)
    }

    @Test
    fun selectingAfterBackAppendsToRemainingHistory() {
        val state = TabNavigationState.restore(listOf("HOME", "POKEMON", "SETTINGS"))

        assertTrue(state.goBack())
        assertTrue(state.select(MainTab.MONSTER_HUNTER))

        assertEquals(listOf("HOME", "POKEMON", "MONSTER_HUNTER"), state.save())
        assertEquals(MainTab.POKEMON, state.previous)
    }

    @Test
    fun saveAndRestorePreserveSelectionAndEntireBackHistory() {
        val original = TabNavigationState.restore(null)
        original.select(MainTab.POKEMON)
        original.select(MainTab.MONSTER_HUNTER)
        original.select(MainTab.SHORTCUTS)
        original.select(MainTab.SETTINGS)
        original.select(MainTab.POKEMON)

        val restored = TabNavigationState.restore(original.save(), MainTab.SHORTCUTS)

        assertEquals(original.save(), restored.save())
        assertEquals(original.current, restored.current)
        assertEquals(original.previous, restored.previous)
        while (original.canGoBack) {
            assertTrue(original.goBack())
            assertTrue(restored.goBack())
            assertEquals(original.current, restored.current)
            assertEquals(original.previous, restored.previous)
        }
        assertFalse(restored.canGoBack)
    }

    @Test
    fun restoringValidStateDoesNotPrependFallback() {
        val state = TabNavigationState.restore(listOf("POKEMON"), MainTab.HOME)

        assertEquals(listOf("POKEMON"), state.save())
        assertEquals(MainTab.POKEMON, state.current)
        assertFalse(state.canGoBack)
    }

    @Test
    fun unknownSavedNamesAreIgnoredWithoutLosingValidEntries() {
        val state = TabNavigationState.restore(
            listOf("REMOVED_TAB", "HOME", "", "POKEMON", "future_tab", "SETTINGS", "404")
        )

        assertEquals(listOf("HOME", "POKEMON", "SETTINGS"), state.save())
        assertEquals(MainTab.SETTINGS, state.current)
        assertEquals(MainTab.POKEMON, state.previous)
    }

    @Test
    fun restoreCleansAdjacentDuplicatesIncludingAcrossUnknownEntries() {
        val state = TabNavigationState.restore(
            listOf("HOME", "HOME", "POKEMON", "REMOVED_TAB", "POKEMON", "SETTINGS", "SETTINGS")
        )

        assertEquals(listOf("HOME", "POKEMON", "SETTINGS"), state.save())
        assertTrue(state.goBack())
        assertEquals(MainTab.POKEMON, state.current)
        assertTrue(state.goBack())
        assertEquals(MainTab.HOME, state.current)
        assertFalse(state.goBack())
    }

    @Test
    fun restoreKeepsNonAdjacentRepeatedTabs() {
        val saved = listOf("HOME", "POKEMON", "HOME", "SETTINGS", "POKEMON")

        val state = TabNavigationState.restore(saved)

        assertEquals(saved, state.save())
    }

    @Test
    fun mutatingSavedSnapshotCannotMutateNavigationState() {
        val state = TabNavigationState.restore(listOf("HOME", "POKEMON"))
        val snapshot = state.save()

        snapshot.clear()
        snapshot.add("SETTINGS")

        assertEquals(listOf("HOME", "POKEMON"), state.save())
        assertEquals(MainTab.POKEMON, state.current)
        assertEquals(MainTab.HOME, state.previous)
        assertTrue(state.canGoBack)
    }

    @Test
    fun changingNavigationStateDoesNotModifyPreviouslySavedSnapshot() {
        val state = TabNavigationState.restore(listOf("HOME", "POKEMON"))
        val snapshot = state.save()

        state.goBack()
        state.select(MainTab.SETTINGS)

        assertEquals(listOf("HOME", "POKEMON"), snapshot)
        assertEquals(listOf("HOME", "SETTINGS"), state.save())
    }

    @Test
    fun mutatingRestoreInputCannotMutateNavigationState() {
        val saved = arrayListOf("HOME", "POKEMON")
        val state = TabNavigationState.restore(saved)

        saved.clear()
        saved.add("SETTINGS")

        assertEquals(listOf("HOME", "POKEMON"), state.save())
        assertEquals(MainTab.POKEMON, state.current)
        assertEquals(MainTab.HOME, state.previous)
    }

    @Test
    fun exactlyFivePrimaryDestinationsExcludeSettings() {
        assertEquals(5, MainTab.entries.count { it.isPrimaryDestination })
        assertFalse(MainTab.SETTINGS.isPrimaryDestination)
        assertTrue(MainTab.PIKMIN.isPrimaryDestination)
    }

    @Test
    fun openingSettingsKeepsSourceHighlightAndBackReturnsToEveryPrimaryPage() {
        MainTab.entries.filter { it.isPrimaryDestination }.forEach { source ->
            val state = TabNavigationState.restore(listOf(source.name))
            assertTrue(state.select(MainTab.SETTINGS))
            assertEquals(MainTab.SETTINGS, state.current)
            assertEquals(source, state.selectedPrimaryTab)
            repeat(3) { assertFalse(state.select(MainTab.SETTINGS)) }
            assertEquals(listOf(source.name, "SETTINGS"), state.save())
            assertTrue(state.goBack())
            assertEquals(source, state.current)
            assertFalse(state.canGoBack)
        }
    }

    @Test
    fun settingsAndItsSourceSurviveSaveAndRestore() {
        val original = TabNavigationState.restore(listOf("HOME", "PIKMIN"))
        original.select(MainTab.SETTINGS)

        val restored = TabNavigationState.restore(original.save())

        assertEquals(MainTab.SETTINGS, restored.current)
        assertEquals(MainTab.PIKMIN, restored.selectedPrimaryTab)
        assertTrue(restored.goBack())
        assertEquals(MainTab.PIKMIN, restored.current)
        assertEquals(MainTab.HOME, restored.previous)
    }

    @Test
    fun selectingHighlightedTabFromSettingsDismissesItWithoutAddingHistory() {
        val state = TabNavigationState.restore(listOf("HOME", "PIKMIN", "SETTINGS"))

        assertTrue(state.select(MainTab.PIKMIN))

        assertEquals(listOf("HOME", "PIKMIN"), state.save())
        assertEquals(MainTab.PIKMIN, state.current)
        assertEquals(MainTab.PIKMIN, state.selectedPrimaryTab)
        assertFalse(state.select(MainTab.PIKMIN))
        assertTrue(state.goBack())
        assertEquals(MainTab.HOME, state.current)
    }

    @Test
    fun selectingDifferentTabFromSettingsDismissesSettingsBeforeNavigating() {
        val state = TabNavigationState.restore(listOf("HOME", "POKEMON", "SETTINGS"))

        assertTrue(state.select(MainTab.PIKMIN))

        assertEquals(listOf("HOME", "POKEMON", "PIKMIN"), state.save())
        assertTrue(state.goBack())
        assertEquals(MainTab.POKEMON, state.current)
    }

    @Test
    fun legacySettingsOnlyStateHasHomeForBothHighlightAndBack() {
        val state = TabNavigationState.restore(listOf("SETTINGS"))

        assertEquals(listOf("HOME", "SETTINGS"), state.save())
        assertEquals(MainTab.SETTINGS, state.current)
        assertEquals(MainTab.HOME, state.selectedPrimaryTab)
        assertTrue(state.goBack())
        assertEquals(MainTab.HOME, state.current)
        assertFalse(state.canGoBack)
    }
}

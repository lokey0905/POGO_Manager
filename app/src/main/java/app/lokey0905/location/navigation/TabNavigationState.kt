package app.lokey0905.location.navigation

/** Stable names survive recreation without persisting generated Android resource IDs. */
enum class MainTab {
    POKEMON, MONSTER_HUNTER, HOME, SHORTCUTS, SETTINGS
}

/** Chronological tab history, independent of Fragment instances and Android Views. */
class TabNavigationState private constructor(private val history: MutableList<MainTab>) {
    val current: MainTab get() = history.last()
    val previous: MainTab? get() = history.getOrNull(history.lastIndex - 1)
    val canGoBack: Boolean get() = history.size > 1

    fun select(tab: MainTab): Boolean {
        if (tab == current) return false
        history.add(tab)
        return true
    }

    fun goBack(): Boolean {
        if (!canGoBack) return false
        history.removeAt(history.lastIndex)
        return true
    }

    fun save(): ArrayList<String> = ArrayList(history.map { it.name })

    companion object {
        fun restore(saved: List<String>?, fallback: MainTab = MainTab.HOME): TabNavigationState {
            val restored = mutableListOf<MainTab>()
            saved.orEmpty().forEach { name ->
                val tab = MainTab.entries.firstOrNull { it.name == name }
                if (tab != null && restored.lastOrNull() != tab) restored.add(tab)
            }
            if (restored.isEmpty()) restored.add(fallback)
            return TabNavigationState(restored)
        }
    }
}

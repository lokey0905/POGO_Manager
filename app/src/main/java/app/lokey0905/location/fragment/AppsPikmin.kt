package app.lokey0905.location.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import app.lokey0905.location.MainActivity
import app.lokey0905.location.R
import app.lokey0905.location.catalog.CatalogApp
import app.lokey0905.location.catalog.CatalogCardsController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.SubtitleCollapsingToolbarLayout

/** Uses the existing retained-Fragment navigation and XML/Material page structure. */
class AppsPikmin : Fragment(R.layout.fragment_apps_pikmin) {
    private var cards: CatalogCardsController? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).bindSettingsToolbar(view)
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        val header = view.findViewById<SubtitleCollapsingToolbarLayout>(R.id.toolbar_layout)
        // Only start a refresh at the expanded top; a downward swipe first expands the header.
        var headerExpanded = true
        view.findViewById<AppBarLayout>(R.id.appBarLayout)
            .addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, offset ->
                headerExpanded = offset == 0
            })
        refresh.setOnChildScrollUpCallback { _, _ ->
            !headerExpanded || view.findViewById<View>(R.id.pikmin_scroll).canScrollVertically(-1)
        }
        cards = CatalogCardsController(
            this, view.findViewById(R.id.pikmin_app_cards), CatalogApp.entries.toList(),
            showSectionHeaders = true
        ) { loading, failures ->
            refresh.isRefreshing = loading
            header.subtitle = getString(
                when {
                    loading -> R.string.catalog_source_loading
                    failures > 0 -> R.string.version_summary_partial_failure
                    else -> R.string.catalog_summary_complete
                }
            )
        }
        refresh.setOnRefreshListener { cards?.refresh(force = true) }
    }

    override fun onResume() {
        super.onResume()
        cards?.refresh()
    }

    override fun onDestroyView() {
        cards = null
        super.onDestroyView()
    }
}

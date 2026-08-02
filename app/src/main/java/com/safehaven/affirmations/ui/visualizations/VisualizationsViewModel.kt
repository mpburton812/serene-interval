package com.safehaven.affirmations.ui.visualizations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safehaven.affirmations.data.AppGraph
import com.safehaven.affirmations.domain.visualizations.CalmingVisualization
import com.safehaven.affirmations.domain.visualizations.CalmingVisualizationCatalog
import com.safehaven.affirmations.domain.visualizations.CalmingVisualizationId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class VisualizationsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsPreferences = AppGraph.settings(application)

    val visualizations: StateFlow<List<CalmingVisualization>> = settingsPreferences.settings
        .map { settings ->
            CalmingVisualizationCatalog.all.filter { settings.enabledScenes.contains(it.id.name) }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CalmingVisualizationCatalog.all,
        )

    fun findVisualization(id: CalmingVisualizationId): CalmingVisualization? =
        CalmingVisualizationCatalog.byId(id)
}

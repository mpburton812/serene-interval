package com.example.meditationparticles.domain.visualizations

import androidx.compose.ui.graphics.Color

enum class CalmingVisualizationId {
    Snowfall,
    Rainfall,
    Firepit,
    Sandblow,
    Leaffall,
}

data class CalmingVisualization(
    val id: CalmingVisualizationId,
    val title: String,
    val subtitle: String,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val cardAccent: Color,
    val sound: SceneAmbientSound,
    val galleryHeightDp: Int,
    val gallerySpan: Int = 1,
)

object CalmingVisualizationCatalog {
    val all: List<CalmingVisualization> = listOf(
        CalmingVisualization(
            id = CalmingVisualizationId.Snowfall,
            title = "Snowfall",
            subtitle = "Winter Stillness",
            backgroundTop = Color(0xFF1A2A3A),
            backgroundBottom = Color(0xFF0D1520),
            cardAccent = Color(0xFFB8D4E8),
            sound = SceneAmbientSound.Wind,
            galleryHeightDp = 220,
            gallerySpan = 2,
        ),
        CalmingVisualization(
            id = CalmingVisualizationId.Rainfall,
            title = "Rainfall",
            subtitle = "Rhythmic Patter",
            backgroundTop = Color(0xFF2A3D35),
            backgroundBottom = Color(0xFF1A2822),
            cardAccent = Color(0xFF8BA88E),
            sound = SceneAmbientSound.Rain,
            galleryHeightDp = 220,
            gallerySpan = 2,
        ),
        CalmingVisualization(
            id = CalmingVisualizationId.Firepit,
            title = "Firepit",
            subtitle = "Warm Embers",
            backgroundTop = Color(0xFF2A1810),
            backgroundBottom = Color(0xFF0A0604),
            cardAccent = Color(0xFFDA8D78),
            sound = SceneAmbientSound.Fire,
            galleryHeightDp = 180,
        ),
        CalmingVisualization(
            id = CalmingVisualizationId.Sandblow,
            title = "Sandblow",
            subtitle = "Fluid Dunes",
            backgroundTop = Color(0xFF4A3A28),
            backgroundBottom = Color(0xFF2A2018),
            cardAccent = Color(0xFFD4A574),
            sound = SceneAmbientSound.Sand,
            galleryHeightDp = 180,
        ),
        CalmingVisualization(
            id = CalmingVisualizationId.Leaffall,
            title = "Leaffall",
            subtitle = "Autumn Drift",
            backgroundTop = Color(0xFF3A2A1A),
            backgroundBottom = Color(0xFF1A1208),
            cardAccent = Color(0xFFC87840),
            sound = SceneAmbientSound.Forest,
            galleryHeightDp = 180,
        ),
    )

    fun byId(id: CalmingVisualizationId): CalmingVisualization? = all.find { it.id == id }

    fun byRouteName(name: String): CalmingVisualization? =
        all.find { it.id.name.equals(name, ignoreCase = true) }
}

package com.safehaven.affirmations.domain.visualizations

import androidx.annotation.RawRes
import com.safehaven.affirmations.R

enum class SceneAmbientSound(
    @RawRes val resourceId: Int,
    val label: String,
) {
    Wind(R.raw.timer_wind, "Wind"),
    Rain(R.raw.timer_rain, "Rain"),
    Waves(R.raw.timer_waves, "Waves"),
    Fire(R.raw.scene_fire, "Fire"),
    Forest(R.raw.timer_forest, "Forest"),
    Sand(R.raw.scene_sand, "Sand"),
}

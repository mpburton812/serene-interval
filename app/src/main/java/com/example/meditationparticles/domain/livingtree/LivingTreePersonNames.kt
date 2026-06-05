package com.example.meditationparticles.domain.livingtree

/**
 * Parses person names from setup bulk-add input.
 * Names are separated by commas; blank segments are ignored.
 */
object LivingTreePersonNames {
    fun parse(input: String): List<String> =
        input.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
}

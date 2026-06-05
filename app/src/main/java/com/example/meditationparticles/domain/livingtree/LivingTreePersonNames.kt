package com.example.meditationparticles.domain.livingtree

/**
 * Parses person names from setup bulk-add input.
 * Names are separated by newlines (one name per line); blank lines are ignored.
 */
object LivingTreePersonNames {
    fun parse(input: String): List<String> =
        input.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
}

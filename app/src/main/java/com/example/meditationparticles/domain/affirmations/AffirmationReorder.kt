package com.example.meditationparticles.domain.affirmations

object AffirmationReorder {
    fun <T> reorder(
        items: List<T>,
        fromIndex: Int,
        toIndex: Int,
    ): List<T> {
        if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) {
            return items
        }
        val mutable = items.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable
    }
}

package com.example.meditationparticles.domain.toolkit

object ToolkitLayout {
    fun defaultOrder(category: ToolkitCategory): List<ToolkitToolId> =
        ToolkitCatalog.byCategory(category).map { it.id }

    fun defaultEnabledTools(): Set<ToolkitToolId> = ToolkitToolId.entries.toSet()

    fun orderedTools(
        category: ToolkitCategory,
        enabledIds: Set<ToolkitToolId>,
        savedOrder: List<ToolkitToolId>,
    ): List<ToolkitTool> {
        val catalogById = ToolkitCatalog.byCategory(category).associateBy { it.id }
        val enabledInCategory = catalogById.keys.filter { it in enabledIds }
        val orderIndex = savedOrder.withIndex().associate { (index, id) -> id to index }
        return enabledInCategory
            .sortedWith(
                compareBy<ToolkitToolId> { orderIndex[it] ?: Int.MAX_VALUE }
                    .thenBy { defaultOrder(category).indexOf(it) },
            )
            .mapNotNull { catalogById[it] }
    }

    fun normalizeOrder(
        category: ToolkitCategory,
        order: List<ToolkitToolId>,
    ): List<ToolkitToolId> {
        val defaults = defaultOrder(category)
        val seen = mutableSetOf<ToolkitToolId>()
        val normalized = order.filter { id ->
            id in defaults && seen.add(id)
        }.toMutableList()
        defaults.forEach { id ->
            if (id !in seen) {
                normalized.add(id)
            }
        }
        return normalized
    }

    fun reorder(
        order: List<ToolkitToolId>,
        fromIndex: Int,
        toIndex: Int,
    ): List<ToolkitToolId> {
        if (fromIndex !in order.indices || toIndex !in order.indices || fromIndex == toIndex) {
            return order
        }
        val mutable = order.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable
    }

    fun randomReactive(enabledIds: Set<ToolkitToolId>): ToolkitTool? =
        orderedTools(
            category = ToolkitCategory.Reactive,
            enabledIds = enabledIds,
            savedOrder = defaultOrder(ToolkitCategory.Reactive),
        ).randomOrNull()
}

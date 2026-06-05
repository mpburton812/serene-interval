package com.example.meditationparticles.domain.toolkit

object ToolkitLayout {
    fun defaultOrder(category: ToolkitCategory): List<ToolkitToolId> =
        ToolkitCatalog.byCategory(category).map { it.id }

    fun defaultEnabledTools(): Set<ToolkitToolId> = ToolkitToolId.entries.toSet()

    fun orderedTools(
        category: ToolkitCategory,
        enabledIds: Set<ToolkitToolId>,
        savedOrder: List<ToolkitToolId>,
        usageCounts: Map<ToolkitToolId, Int> = emptyMap(),
    ): List<ToolkitTool> {
        val catalogById = ToolkitCatalog.byCategory(category).associateBy { it.id }
        val enabledInCategory = catalogById.keys.filter { it in enabledIds }
        return sortByUsage(
            toolIds = enabledInCategory,
            category = category,
            savedOrder = savedOrder,
            usageCounts = usageCounts,
        ).mapNotNull { catalogById[it] }
    }

    fun sortByUsage(
        toolIds: List<ToolkitToolId>,
        category: ToolkitCategory,
        savedOrder: List<ToolkitToolId>,
        usageCounts: Map<ToolkitToolId, Int>,
    ): List<ToolkitToolId> {
        val savedOrderIndex = savedOrder.withIndex().associate { (index, id) -> id to index }
        val defaultOrderIndex = defaultOrder(category).withIndex().associate { (index, id) -> id to index }
        return toolIds.sortedWith(
            compareByDescending<ToolkitToolId> { usageCounts[it] ?: 0 }
                .thenBy { savedOrderIndex[it] ?: Int.MAX_VALUE }
                .thenBy { defaultOrderIndex[it] ?: Int.MAX_VALUE },
        )
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

package com.safehaven.affirmations.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class LivingTreePersonWithTags(
    @Embedded val person: LivingTreePersonEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = LivingTreePersonTagCrossRef::class,
            parentColumn = "personId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<LivingTreeTagEntity>,
)

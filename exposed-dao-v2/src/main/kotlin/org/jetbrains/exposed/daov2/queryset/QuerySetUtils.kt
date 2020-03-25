package org.jetbrains.exposed.daov2.queryset

import org.jetbrains.exposed.daov2.manager.EntityManager
import org.jetbrains.exposed.sql.ColumnSet
import org.jetbrains.exposed.sql.innerJoin


fun EntityManager<*, *, *>.addRelatedJoin(newJoin: (ColumnSet.() -> ColumnSet)) {
    val parent = this.relatedColumnId?.table as EntityManager<*, *, *>?
    if(parent != null) {
        parent.addRelatedJoin(newJoin)
    } else {
        val relatedJoin = this.relatedJoin
        this.relatedJoin = {
            val columnset = relatedJoin?.let { this.it() } ?: this
            columnset.newJoin()
        }
    }
}

fun EntityManager<*, *, *>.joinWithParent() {
    val parent = this.relatedColumnId?.table as EntityManager<*, *, *>?

    parent?.let {
        parent.joinWithParent() // join parent with his parent
    }

    this.aliasRelated?.let { alias -> // join me with my parent
        val parentAlias = parent?.aliasRelated
        addRelatedJoin { innerJoin(alias, { alias[relatedColumnId?.referee!!] }, { parentAlias?.let { it[relatedColumnId!!] } ?: relatedColumnId!! }) }
    }
}

package org.jetbrains.exposed.daov2.queryset

import org.jetbrains.exposed.daov2.manager.EntityManager
import org.jetbrains.exposed.sql.*


fun EntityManager<*, *, *>.addRelatedJoin(table: Table, newJoin: (ColumnSet.() -> ColumnSet)) {
    if(parent != null) {
        parent!!.addRelatedJoin(table, newJoin)
    } else {
        this.relatedJoin[table] = newJoin
    }
}

fun joinWith(leftTable: Table, rightTable: Table, column: Column<*>): (ColumnSet.() -> ColumnSet) {
    val originalLeftTable = if(leftTable is Alias<*>) leftTable.delegate else leftTable

    val rightOn: Column<*>
    val leftOn: Column<*>

    if(column.table == originalLeftTable) {
        leftOn = if(leftTable is Alias<*>) leftTable[column] else column
        rightOn = if(rightTable is Alias<*>) rightTable[column.referee!!] else column.referee!!
    } else {
        leftOn = if(leftTable is Alias<*>) leftTable[column.referee!!] else column.referee!!
        rightOn = if(rightTable is Alias<*>) rightTable[column] else column
    }
    return {
        innerJoin(rightTable, { leftOn }, { rightOn })
    }
}




fun EntityManager<*, *, *>.joinWithParent() {
    parent?.let { leftTable ->
        leftTable.joinWithParent() // join parent with his parent
        val parentAlias = leftTable.aliasRelated

        addRelatedJoin(this.aliasRelated!!,
                joinWith(parentAlias?:leftTable, this.aliasRelated!!, this.relatedColumnId!!)
        )
    }
}



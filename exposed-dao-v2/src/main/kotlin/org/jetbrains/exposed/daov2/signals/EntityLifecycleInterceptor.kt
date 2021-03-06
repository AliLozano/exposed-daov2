package org.jetbrains.exposed.daov2.signals

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.daov2.manager.transactionCache
import org.jetbrains.exposed.daov2.manager.flushCache
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.*
import org.jetbrains.exposed.sql.targetTables

class EntityLifecycleInterceptor : GlobalStatementInterceptor {

    override fun beforeExecution(transaction: Transaction, context: StatementContext) {
        when (val statement = context.statement) {
            is Query -> transaction.flushEntities(statement)

            is DeleteStatement -> {
                transaction.flushCache()
            }

            is InsertStatement<*> -> {
                transaction.flushCache()
            }

            is BatchUpdateStatement -> {}

            is UpdateStatement -> {
                transaction.flushCache()
            }

            else -> {
                if(statement.type.group == StatementGroup.DDL)
                    transaction.flushCache()
            }
        }
    }

    override fun beforeCommit(transaction: Transaction) {
        transaction.flushCache()
        transaction.alertSubscribers()
        transaction.flushCache()
    }

    override fun beforeRollback(transaction: Transaction) = transaction.transactionCache.clear()

    private fun Transaction.flushEntities(query: Query) {
        // Flush data before executing query or results may be unpredictable
        val tables = query.set.source.columns.map { it.table }.filterIsInstance(IdTable::class.java).toSet()
        transactionCache.flush(tables)
    }
}
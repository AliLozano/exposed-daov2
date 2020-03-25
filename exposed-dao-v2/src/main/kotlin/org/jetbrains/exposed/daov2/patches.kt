package org.jetbrains.exposed.daov2

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.wrap
import org.jetbrains.exposed.sql.statements.DeleteStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

// https://github.com/JetBrains/Exposed/pull/830
operator fun ExpressionWithColumnType<String>.plus(t: String) = SqlExpressionBuilder.concat(this, wrap(t))

// https://github.com/JetBrains/Exposed/issues/451
fun Table.deleteWhere(limit: Int? = null, offset: Long? = null, op: SqlExpressionBuilder.()-> Op<Boolean>) =
        DeleteStatement.where(TransactionManager.current(), this@deleteWhere, SqlExpressionBuilder.op(), false, limit, offset?.toInt())

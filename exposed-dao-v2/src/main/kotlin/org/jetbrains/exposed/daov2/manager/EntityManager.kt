package org.jetbrains.exposed.daov2.manager

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.daov2.entities.Entity
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.daov2.exceptions.EntityNotFoundException
import org.jetbrains.exposed.daov2.queryset.EntityQueryBase
import org.jetbrains.exposed.daov2.queryset.joinWithParent
import org.jetbrains.exposed.daov2.queryset.localTransaction
import org.jetbrains.exposed.daov2.signals.EntityChangeType
import org.jetbrains.exposed.daov2.signals.registerChange
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager

val transactionExist get() = TransactionManager.isInitialized() && TransactionManager.currentOrNull() != null

/**
 * Create a new entity with the fields that are set in the [init] block. The id will be automatically set.
 *
 * @param init The block where the entities' fields can be set.
 *
 * @return The entity that has been created.
 */
fun <ID : Comparable<ID>, E : Entity<ID>> EntityManager<ID, E, *>.new(init: E.() -> Unit) = localTransaction {
    createInstance().apply {
        this.init()
        this.save()
    }
}

/**
 * Reloads entity fields from database as new object.
 * @param flush whether pending entity changes should be flushed previously
 */
fun <ID : Comparable<ID>, E : Entity<ID>> EntityManager<ID, E, *>.reload(entity: E, flush: Boolean = false): E = entity.also {
    localTransaction {
        if (flush) {
            cache.flush()
        }
        entity.reset()
        entity.readValues = findResultRowById(entity.id) ?: throw EntityNotFoundException(entity.id, this@reload)
        cache[this@reload].store(entity)
    }
}

interface CopiableObject<M: CopiableObject<M>> {
    fun copy(): M
}

typealias JoinFunction= (ColumnSet.() -> ColumnSet)

@Suppress("UNCHECKED_CAST")
abstract class EntityManager<ID : Comparable<ID>, E : Entity<ID>, M : EntityManager<ID, E, M>>(name: String = "") : IdTable<ID>(), ISqlExpressionBuilder, CopiableObject<M> {
    val originalId: Column<EntityID<ID>> get() = this.id.referee() ?: this.id

    private val klass = this.javaClass.enclosingClass

    private val ctor = klass.constructors.first()

    val objects: EntityQueryBase<ID, E, M> = this.buildEntityQuery()

    var relatedColumnId: Column<Any>? = null

    var relatedJoin: MutableMap<Table, JoinFunction> = mutableMapOf() // se utiliza para hacer los joins

    val aliasRelated by lazy {
        // tener en cuenta que si se llama al alias related antes de setear el related column id, este se quedara como null
        relatedColumnId?.let {
            Alias(this, "${it.name}_${this.tableName}")
        }
    }

    var parent: EntityManager<*, *, *>? = null

    override var tableName = name.ifBlank { klass.simpleName!! }

    internal open val cache get() = TransactionManager.current().transactionCache

    open fun createInstance() = ctor.newInstance() as E

    open val defaultQuery get() = this.selectAll()

    fun relatedJoinQuery(query: Query): Query? {
        val queryResult = query.copy()
        return aliasRelated?.let { thisAlias ->
            val parent = relatedColumnId!!.table as EntityManager<*,*, *>
            val parentId = parent.aliasRelated?.let { it[relatedColumnId!!] } ?: relatedColumnId!!
            thisAlias.innerJoin(parent.relatedJoinQuery(queryResult)?.set?.source ?: parent, { thisAlias[relatedColumnId!!.referee!!] }, { parentId }).selectAll()
        }
    }


    fun asRelatedTable(column: Column<Any>, parent: EntityManager<*,*,*> = column.table as EntityManager<*,*,*>) {
        this.relatedColumnId = column
        this.parent = parent
    }

    open fun findResultRowById(id: EntityID<ID>): ResultRow? = localTransaction {
        select(this@EntityManager.originalId eq id).firstOrNull()
    }



    open fun save(prototype: E): E = prototype.also {
        localTransaction {
            cache.scheduleSave(this@EntityManager, prototype)
        }
    }

    open fun delete(id: EntityID<ID>) = localTransaction {
        objects.filter { this@EntityManager.id eq id }.delete()
        cache[this@EntityManager].remove(id)
        TransactionManager.current().registerChange(this@EntityManager, id, EntityChangeType.Removed)
    }


    fun buildEntityQuery(initQuery: Query? = null): EntityQueryBase<ID, E, M> = EntityQueryBase(this as M, initQuery ?: defaultQuery)

    override fun copy(): M = this.javaClass.constructors.first().let {
        it.isAccessible = true
        it.newInstance(null) as M
    }

}
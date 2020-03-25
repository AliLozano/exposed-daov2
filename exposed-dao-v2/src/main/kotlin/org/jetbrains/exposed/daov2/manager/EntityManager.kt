package org.jetbrains.exposed.daov2.manager

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.daov2.entities.Entity
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.daov2.deleteWhere
import org.jetbrains.exposed.daov2.entities.getValue
import org.jetbrains.exposed.daov2.exceptions.EntityNotFoundException
import org.jetbrains.exposed.daov2.queryset.EntityQueryBase
import org.jetbrains.exposed.daov2.queryset.joinWithParent
import org.jetbrains.exposed.daov2.queryset.localTransaction
import org.jetbrains.exposed.daov2.signals.EntityChangeType
import org.jetbrains.exposed.daov2.signals.registerChange
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.util.*
import kotlin.reflect.KClass
import kotlin.sequences.Sequence

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

@Suppress("UNCHECKED_CAST")
abstract class EntityManagerBase<ID : Comparable<ID>, E : Entity<ID>>(name: String = "") : IdTable<ID>(), SqlExpressionBuilderClass {
    val originalId: Column<EntityID<ID>> get() = this.id.referee() ?: this.id

    private val klass = this.javaClass.enclosingClass
    private val ctor = klass.constructors.first()
    abstract val objects: EntityQueryBase<ID, E, *>

    var relatedColumnId: Column<Any>? = null

    var relatedJoin: (ColumnSet.() -> ColumnSet)? = null // se utiliza para hacer los joins

    val aliasRelated by lazy {
        // tener en cuenta que si se llama al alias related antes de setear el related column id, este se quedara como null
        relatedColumnId?.let {
            Alias(this, "${it.name}_${this.tableName}")
        }
    }

    val parent get() = this.relatedColumnId?.table as EntityManager<*, *, *>?

    override var tableName = name.ifBlank { klass.simpleName!! }

    internal open val cache get() = TransactionManager.current().transactionCache

    open fun createInstance() = ctor.newInstance() as E

    open val defaultQuery by lazy { this.selectAll() }

    private fun relatedQuery(): Query? {
        return aliasRelated?.let {thisAlias ->
            val parent = relatedColumnId!!.table as EntityManager<*,*, *>
            val parentId = parent.aliasRelated?.let { it[relatedColumnId!!] } ?: relatedColumnId!!
            this.innerJoin(parent.defaultQuery.set.source, { thisAlias[relatedColumnId!!.referee!!] }, { parentId }).selectAll()
        }
    }


    fun asRelatedTable(column: Column<Any>) { this.relatedColumnId = column }

    open fun findResultRowById(id: EntityID<ID>): ResultRow? = localTransaction {
        select(this@EntityManagerBase.originalId eq id).firstOrNull()
    }

    open fun save(prototype: E): E = prototype.also {
        localTransaction {
            cache.scheduleSave(this@EntityManagerBase, prototype)
        }
    }

    open fun delete(id: EntityID<ID>) = localTransaction {
        objects.filter { this@EntityManagerBase.id eq id }.delete()
        cache[this@EntityManagerBase].remove(id)
        TransactionManager.current().registerChange(this@EntityManagerBase, id, EntityChangeType.Removed)
    }



}



@Suppress("UNCHECKED_CAST")
abstract class EntityManager<ID : Comparable<ID>, E : Entity<ID>, M : EntityManager<ID, E, M>>(name: String = "") : EntityManagerBase<ID, E>(), CopiableObject<M> {
    override val objects: EntityQueryBase<ID, E, M> = EntityQueryBase(this as M)

    override fun copy(): M = this.javaClass.constructors.first().let {
        it.isAccessible = true
        it.newInstance(null) as M
    }
}
package org.jetbrains.exposed.daov2.entities

import org.jetbrains.exposed.daov2.manager.EntityManager
import org.jetbrains.exposed.daov2.manager.MutableResultRow
import org.jetbrains.exposed.daov2.manager.reload
import org.jetbrains.exposed.sql.Column
import kotlin.reflect.full.companionObjectInstance

enum class FlushAction {
    NONE,
    INSERT,
    UPDATE,
}

open class Entity<ID : Comparable<ID>> : EntityDelegator<ID>,MutableResultRow<ID>() {

    /* Getters */
    @Suppress("UNCHECKED_CAST")
    override val table: EntityManager<ID, Entity<ID>, *>
        get() = this::class.companionObjectInstance as EntityManager<ID, Entity<ID>, *>


    override fun <T> Column<*>.lookup(): T {
        return this@Entity.getValue(this)
    }

    override fun <T> Column<*>.putValue(value: T) {
        this@Entity.setValue(this, value)
    }

    /**
     * Delete this entity.
     *
     * This will remove the entity from the database as well as the cache.
     */
    open fun delete() {
        table.delete(this.id)
    }

    open fun save(): Entity<ID> {
        return this.table.save(this)
    }

    open fun reload() {
        this.table.reload(this, true)
    }
}



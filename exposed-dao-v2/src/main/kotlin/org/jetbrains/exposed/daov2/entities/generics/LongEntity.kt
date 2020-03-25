package org.jetbrains.exposed.daov2.entities.generics

import org.jetbrains.exposed.daov2.entities.Entity
import org.jetbrains.exposed.daov2.entities.getValue
import org.jetbrains.exposed.daov2.manager.EntityManager

abstract class LongEntity : Entity<Long>()

open class LongEntityManager<T : Entity<Long>, M: EntityManager<Long, T, M>>(name: String = "", columnName: String = "id") : EntityManager<Long, T, M>(name) {
    override val id by long(columnName).autoIncrement().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}


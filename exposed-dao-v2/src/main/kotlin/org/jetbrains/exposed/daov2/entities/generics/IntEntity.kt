package org.jetbrains.exposed.daov2.entities.generics

import org.jetbrains.exposed.daov2.entities.Entity
import org.jetbrains.exposed.daov2.entities.getValue
import org.jetbrains.exposed.daov2.manager.EntityManager

abstract class IntEntity : Entity<Int>()

open class IntEntityManager<E : Entity<Int>, M: EntityManager<Int, E, M>>(name: String = "", columnName: String = "id") : EntityManager<Int, E, M>(name) {
    override val id by integer(columnName).autoIncrement().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}


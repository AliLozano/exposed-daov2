package org.jetbrains.exposed.daov2.entities.generics

import org.jetbrains.exposed.daov2.entities.Entity
import org.jetbrains.exposed.daov2.entities.getValue
import org.jetbrains.exposed.daov2.manager.EntityManager
import java.util.*

abstract class UUIDEntity : Entity<UUID>()

open class UUIDEntityManager<T : Entity<UUID>, M: EntityManager<UUID, T, M>>(name: String = "", columnName: String = "id") : EntityManager<UUID, T, M>(name) {
    override val id by uuid(columnName).autoGenerate().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(originalId) }
}


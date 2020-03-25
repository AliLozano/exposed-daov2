package org.jetbrains.exposed.daov2.exceptions

import org.jetbrains.exposed.daov2.manager.EntityManager
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

class EntityNotFoundException(val id: EntityID<*>, val entity: IdTable<*>)
    : Exception("Entity ${entity::class.simpleName}, id=$id not found in the database")

package org.jetbrains.exposed.daov2

import org.jetbrains.exposed.daov2.entities.*
import org.jetbrains.exposed.daov2.entities.generics.IntEntity
import org.jetbrains.exposed.daov2.entities.generics.IntEntityManager
import org.jetbrains.exposed.daov2.manager.EntityManager
import org.jetbrains.exposed.daov2.manager.oneToMany
import org.jetbrains.exposed.daov2.manager.oneToManyRef
import org.jetbrains.exposed.daov2.queryset.EntityQuery
import org.jetbrains.exposed.daov2.queryset.EntityQueryBase
import org.jetbrains.exposed.daov2.queryset.EntitySizedIterable
import org.jetbrains.exposed.sql.Query
import kotlin.reflect.KProperty


open class CountryTable: IntEntityManager<Country, CountryTable>() {
    val name by varchar("name", 255)
}

class Country: IntEntity() {
    companion object Table: CountryTable()
    var name by Table.name
}


abstract class RegionTable: IntEntityManager<Region, RegionTable>() {
    val name by varchar("name", 255)
    val country by manyToOne("country", Country)
}

class Region: IntEntity() {
    companion object Table: RegionTable()
    var name by Table.name
    var country by Table.country
}

open class SchoolTable: IntEntityManager<School, SchoolTable>() {
    val name by varchar("name", 255)
    val region by manyToOne("region_id", Region)
    val secondaryRegion by manyToOptional("secondary_region_id", Region)
}



class School : IntEntity() {
    companion object Table: SchoolTable()

    var name by Table.name
    var region by Table.region
    var secondaryRegion by Table.secondaryRegion.nullable()
}

val RegionTable.schools by oneToManyRef(School.region, School)
// val RegionTable.schoolsSecondary by oneToManyRef(School.secondaryRegion, School)

val Region.schools by Region.schools.oneToMany()
// val Region.schoolsSecondary by Region.schoolsSecondary.oneToMany()
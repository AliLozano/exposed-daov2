package org.jetbrains.exposed.daov2

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.daov2.entities.*
import org.jetbrains.exposed.daov2.entities.generics.IntEntity
import org.jetbrains.exposed.daov2.entities.generics.IntEntityManager
import org.jetbrains.exposed.daov2.manager.new
import org.jetbrains.exposed.daov2.queryset.first
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityBeanTest {

    @Test
    fun `Create entity object with empty constructor`() {
        val region = Region()
        region.name = "Region"
        assertThat(region.name).isEqualTo("Region")
    }

    @Test
    fun `Create entity with reference`() {
        val region = Region()
        region.name = "Region"
        val school = School()
        school.region = region
        assertThat(school.region.name).isEqualTo("Region")
    }
}
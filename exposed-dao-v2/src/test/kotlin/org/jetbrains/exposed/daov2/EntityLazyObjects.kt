package org.jetbrains.exposed.daov2

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.daov2.entities.generics.IntEntity
import org.jetbrains.exposed.daov2.entities.generics.IntEntityManager
import org.jetbrains.exposed.daov2.entities.getValue
import org.jetbrains.exposed.daov2.entities.manyToOne
import org.jetbrains.exposed.daov2.manager.new
import org.jetbrains.exposed.daov2.queryset.first
import org.jetbrains.exposed.daov2.queryset.last
import org.jetbrains.exposed.daov2.queryset.localTransaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityLazyObjects {
    @BeforeEach
    fun before() {
        Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;")

        transaction {
            SchemaUtils.create(Country, Region, School)
        }
    }

    @AfterEach
    fun after() {
        transaction {
            SchemaUtils.drop(Country, Region, School)
        }
    }

    @Test
    fun `Test dont load lazy object on id`() {
        School.new { name = "School"; region = Region.new { name = "Region"; country = Country.new  { name="Country" }} }

        val school = School.objects.first()
        assertThat(school.region.id.value).isEqualTo(1)
    }

    @Test
    fun `Test lazy objects`() {
        School.new { name = "School"; region = Region.new { name = "Region"; country = Country.new  { name="Country" }} }

        val school = School.objects.first()
        assertThat(school.region.name).isEqualTo("Region")
        assertThat(school.region.country.name).isEqualTo("Country")
    }

    @Test
    fun `Test lazy two objects`() {
        School.new { name = "School"; region = Region.new { name = "Region"; country = Country.new  { name="Country" }} }
        val school = School.objects.first()
        assertThat(school.region.country.name).isEqualTo("Country")
    }


    @Test
    fun `Test lazy related querys`() {
        School.new { name = "School"; region = Region.new { name = "Region"; country = Country.new  { name="Country" }} }

        transaction {
            val school = School.objects.first()
            Region.objects.all().iterator()
            School.objects.all().iterator()
            Country.objects.all().iterator()
            assertThat(school.region.country.name).isEqualTo("Country")
        }
    }

    @Test
    fun `Test lazy select related querys`() {
        val c1 = Country.new  { name="Country" }
        val r = Region.new { name = "Region"; country = c1}
        val r2 = Region.new { name = "Region 2"; country = c1}
        School.new { name = "School"; region = r }
        School.new { name = "School"; region = r }
        School.new { name = "School"; region = r2 }
        School.new { name = "School"; region = r2 }
        val school = School.objects.selectRelated(School.region, School.region.country).first()
        //assertThat(school.region.country.name).isEqualTo("Country")
    }
}


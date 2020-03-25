package org.jetbrains.exposed.daov2

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.daov2.manager.new
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityManyToOneTest {


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
    fun `Test that that we can use id column of manager in querys without inner join`() {
        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        Region.new { name = "Lima"; country = peru }

        Region.objects.filter { country.id eq 1 }

        transaction {
            val result = Region.selectAll().first()
            assertThat(result[Region.country.id]).isEqualTo(peru.id)
        }
    }

    @Test
    fun `Test simple inner join`() {
        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        Region.new { name = "Lima"; country = peru }

        val result = Region.objects.filter { country.name eq "Peru" }.count()
        assertThat(result).isEqualTo(1)
    }

    @Test
    fun `Test double inner join`() {
        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        val lima = Region.new { name = "Lima"; country = peru }
        val trujillo = Region.new { name = "Trujillo"; country = peru }

        School.new { name = "School 1"; region = lima }
        School.new { name = "School 2"; region = lima }

        val result = School.objects.filter { region.country.name eq "Peru" }.count()
        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `Test with nullable relations`() {
        Country.new { name = "Argentina" }
        Country.new { name = "Australia" }
        val peru = Country.new { name = "Peru" }
        val lima = Region.new { name = "Lima"; country = peru }
        val trujillo = Region.new { name = "Trujillo"; country = peru }

        School.new { name = "School 1"; region = lima }
        School.new { name = "School 2"; region = lima; secondaryRegion = null }
        School.new { name = "School 2"; region = lima; secondaryRegion = trujillo }

        School.objects.filter { secondaryRegion.id.isNull() }.count().also {
            assertThat(it).isEqualTo(2)
        }

        School.objects.filter { region.id eq lima.id and secondaryRegion.id.isNull() }.count().also {
            assertThat(it).isEqualTo(2)
        }
    }

}
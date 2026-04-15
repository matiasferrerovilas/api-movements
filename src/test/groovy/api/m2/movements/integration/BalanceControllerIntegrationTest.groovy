package api.m2.movements.integration

import api.m2.movements.entities.Movement
import api.m2.movements.enums.MovementType
import api.m2.movements.repositories.MovementRepository
import org.springframework.beans.factory.annotation.Autowired

import java.time.LocalDate

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class BalanceControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Autowired
    MovementRepository movementRepository

    def "GET /v1/balance - should return balance with ingreso and gasto"() {
        given:
        def category = getOrCreateCategory("SIN_CATEGORIA")
        def today = LocalDate.now()

        // Create INGRESO movement
        movementRepository.save(Movement.builder()
                .amount(new BigDecimal("1000.00"))
                .description("Salary")
                .type(MovementType.INGRESO)
                .date(today)
                .owner(testUser)
                .workspace(testWorkspace)
                .currency(testCurrency)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        // Create DEBITO movement
        movementRepository.save(Movement.builder()
                .amount(new BigDecimal("300.00"))
                .description("Grocery shopping")
                .type(MovementType.DEBITO)
                .date(today)
                .owner(testUser)
                .workspace(testWorkspace)
                .currency(testCurrency)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        when:
        def result = mockMvc.perform(get("/v1/balance")
                .with(jwtAuth())
                .param("startDate", today.minusDays(1).toString())
                .param("endDate", today.plusDays(1).toString())
                .param("currencies", "ARS"))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.INGRESO').value(1000.0))
                .andExpect(jsonPath('$.GASTO').value(300.0))
    }

    def "GET /v1/balance - should return zeros when no movements exist"() {
        when:
        def result = mockMvc.perform(get("/v1/balance")
                .with(jwtAuth())
                .param("currencies", "ARS"))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.INGRESO').value(0))
                .andExpect(jsonPath('$.GASTO').value(0))
    }

    def "GET /v1/balance/category - should return balance grouped by category"() {
        given:
        def comidaCategory = getOrCreateCategory("COMIDA")
        def transporteCategory = getOrCreateCategory("TRANSPORTE")
        def today = LocalDate.now()

        movementRepository.save(Movement.builder()
                .amount(new BigDecimal("200.00"))
                .description("Food expense")
                .type(MovementType.DEBITO)
                .date(today)
                .owner(testUser)
                .workspace(testWorkspace)
                .currency(testCurrency)
                .category(comidaCategory)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        movementRepository.save(Movement.builder()
                .amount(new BigDecimal("100.00"))
                .description("Transport expense")
                .type(MovementType.DEBITO)
                .date(today)
                .owner(testUser)
                .workspace(testWorkspace)
                .currency(testCurrency)
                .category(transporteCategory)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        when:
        def result = mockMvc.perform(get("/v1/balance/category")
                .with(jwtAuth())
                .param("startDate", today.minusDays(1).toString())
                .param("endDate", today.plusDays(1).toString())
                .param("currencies", "ARS"))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$').isArray())
    }

    def "GET /v1/balance/group - should return balance by workspace"() {
        given:
        def category = getOrCreateCategory("SIN_CATEGORIA")
        def today = LocalDate.now()

        movementRepository.save(Movement.builder()
                .amount(new BigDecimal("500.00"))
                .description("Workspace expense")
                .type(MovementType.DEBITO)
                .date(today)
                .owner(testUser)
                .workspace(testWorkspace)
                .currency(testCurrency)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        when:
        def result = mockMvc.perform(get("/v1/balance/group")
                .with(jwtAuth())
                .param("year", today.year.toString())
                .param("month", today.monthValue.toString()))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$').isArray())
    }

    def "GET /v1/balance/monthly-evolution - should return monthly evolution"() {
        given:
        def category = getOrCreateCategory("SIN_CATEGORIA")
        def today = LocalDate.now()

        movementRepository.save(Movement.builder()
                .amount(new BigDecimal("400.00"))
                .description("Monthly expense")
                .type(MovementType.DEBITO)
                .date(today)
                .owner(testUser)
                .workspace(testWorkspace)
                .currency(testCurrency)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        when:
        def result = mockMvc.perform(get("/v1/balance/monthly-evolution")
                .with(jwtAuth())
                .param("year", today.year.toString()))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$').isArray())
    }

    def "GET /v1/balance - should require authentication"() {
        when:
        def result = mockMvc.perform(get("/v1/balance"))

        then:
        result.andExpect(status().isUnauthorized())
    }

    def "GET /v1/balance/category - should require authentication"() {
        when:
        def result = mockMvc.perform(get("/v1/balance/category"))

        then:
        result.andExpect(status().isUnauthorized())
    }

    def "GET /v1/balance/group - should require authentication"() {
        when:
        def result = mockMvc.perform(get("/v1/balance/group")
                .param("year", "2024")
                .param("month", "1"))

        then:
        result.andExpect(status().isUnauthorized())
    }

    def "GET /v1/balance/monthly-evolution - should require authentication"() {
        when:
        def result = mockMvc.perform(get("/v1/balance/monthly-evolution")
                .param("year", "2024"))

        then:
        result.andExpect(status().isUnauthorized())
    }
}

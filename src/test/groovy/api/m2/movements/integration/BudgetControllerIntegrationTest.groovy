package api.m2.movements.integration

import api.m2.movements.entities.movements.Movement
import api.m2.movements.enums.MovementType
import api.m2.movements.records.BudgetToAdd
import api.m2.movements.repositories.MovementRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

import java.time.LocalDate

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class BudgetControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Autowired
    MovementRepository movementRepository

    def "GET /v1/budgets - should return budget with spent amount calculated from movements"() {
        given:
        def category = getOrCreateCategory("SUPERMERCADO")
        def today = LocalDate.now()

        movementRepository.saveAndFlush(Movement.builder()
                .amount(new BigDecimal("200.00"))
                .description("Compra super")
                .type(MovementType.DEBITO)
                .date(today)
                .ownerId(testUserId)
                .workspaceId(testWorkspaceId)
                .currency(testCurrency)
                .categories([category] as Set)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        def request = [
                category: "SUPERMERCADO",
                currency: "ARS",
                amount  : 1000.00,
                year    : today.year,
                month   : today.monthValue
        ]

        mockMvc.perform(post("/v1/budgets")
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())

        when:
        def result = mockMvc.perform(get("/v1/budgets")
                .with(jwtAuth())
                .param("currency", "ARS")
                .param("year", today.year.toString())
                .param("month", today.monthValue.toString()))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$[0].spent').value(200.0))
    }

    def "GET /v1/budgets - should return zero spent when no movements match the category"() {
        given:
        getOrCreateCategory("SIN_GASTOS")
        def today = LocalDate.now()
        def request = [
                category: "SIN_GASTOS",
                currency: "ARS",
                amount  : 500.00,
                year    : today.year,
                month   : today.monthValue
        ]

        mockMvc.perform(post("/v1/budgets")
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())

        when:
        def result = mockMvc.perform(get("/v1/budgets")
                .with(jwtAuth())
                .param("currency", "ARS")
                .param("year", today.year.toString())
                .param("month", today.monthValue.toString()))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$[0].spent').value(0))
    }

    def "GET /v1/budgets - should require authentication"() {
        when:
        def result = mockMvc.perform(get("/v1/budgets")
                .param("year", "2026")
                .param("month", "1"))

        then:
        result.andExpect(status().isUnauthorized())
    }
}

package api.m2.movements.integration

import api.m2.movements.entities.Bank
import api.m2.movements.entities.Movement
import api.m2.movements.enums.MovementType
import api.m2.movements.repositories.BankRepository
import api.m2.movements.repositories.MovementRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

import java.time.LocalDate

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class MovementControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Autowired
    MovementRepository movementRepository

    @Autowired
    BankRepository bankRepository

    def "GET /v1/expenses - should return paginated movements"() {
        given:
        def category = getOrCreateCategory("SIN_CATEGORIA")
        movementRepository.saveAndFlush(Movement.builder()
                .amount(new BigDecimal("100.00"))
                .description("Test expense")
                .type(MovementType.DEBITO)
                .date(LocalDate.now())
                .owner(testUser)
                .workspace(testWorkspace)
                .currency(testCurrency)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        when:
        def result = mockMvc.perform(get("/v1/expenses")
                .with(jwtAuth())
                .param("page", "0")
                .param("size", "10"))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.content').isArray())
    }

    def "GET /v1/expenses - should return empty page when no movements"() {
        when:
        def result = mockMvc.perform(get("/v1/expenses")
                .with(jwtAuth())
                .param("page", "0")
                .param("size", "10"))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.content').isArray())
                .andExpect(jsonPath('$.content.length()').value(0))
    }

    def "POST /v1/expenses - should create movement and return 201"() {
        given:
        def category = getOrCreateCategory("COMIDA")
        def bank = bankRepository.findByDescription("GALICIA")
                .orElseGet { bankRepository.save(Bank.builder().description("GALICIA").build()) }

        def request = [
                amount     : 250.50,
                date       : LocalDate.now().toString(),
                description: "Almuerzo",
                category   : "COMIDA",
                type       : "DEBITO",
                currency   : "ARS",
                cuotaActual: 0,
                cuotaTotal : 0,
                bank       : "GALICIA"
        ]

        when:
        def result = mockMvc.perform(post("/v1/expenses")
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isCreated())
                .andExpect(jsonPath('$.description').value("Almuerzo"))
                .andExpect(jsonPath('$.amount').value(250.5))
                .andExpect(jsonPath('$.type').value("DEBITO"))
    }

    def "POST /v1/expenses - should return 400 for invalid data"() {
        given:
        def request = [
                amount     : null, // Required field missing
                date       : LocalDate.now().toString(),
                description: "", // Empty description
                category   : "COMIDA",
                type       : "DEBITO",
                currency   : "ARS"
        ]

        when:
        def result = mockMvc.perform(post("/v1/expenses")
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isBadRequest())
    }

    def "PATCH /v1/expenses/{id} - should update movement"() {
        given:
        def category = getOrCreateCategory("SIN_CATEGORIA")
        def movement = movementRepository.save(Movement.builder()
                .amount(new BigDecimal("100.00"))
                .description("Original description")
                .type(MovementType.DEBITO)
                .date(LocalDate.now())
                .owner(testUser)
                .workspace(testWorkspace)
                .currency(testCurrency)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        def updateRequest = [
                description: "Updated description",
                amount     : 150.00
        ]

        when:
        def result = mockMvc.perform(patch("/v1/expenses/{id}", movement.id)
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))

        then:
        result.andExpect(status().isNoContent())

        and:
        def updated = movementRepository.findById(movement.id).get()
        updated.description == "Updated description"
        updated.amount == new BigDecimal("150.00")
    }

    def "DELETE /v1/expenses/{id} - should delete movement and return 204"() {
        given:
        def category = getOrCreateCategory("SIN_CATEGORIA")
        def movement = movementRepository.save(Movement.builder()
                .amount(new BigDecimal("100.00"))
                .description("To be deleted")
                .type(MovementType.DEBITO)
                .date(LocalDate.now())
                .owner(testUser)
                .workspace(testWorkspace)
                .currency(testCurrency)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        when:
        def result = mockMvc.perform(delete("/v1/expenses/{id}", movement.id)
                .with(jwtAuth()))

        then:
        result.andExpect(status().isNoContent())

        and:
        !movementRepository.findById(movement.id).isPresent()
    }

    def "DELETE /v1/expenses/{id} - should return 404 for non-existent movement"() {
        when:
        def result = mockMvc.perform(delete("/v1/expenses/{id}", 999999L)
                .with(jwtAuth()))

        then:
        result.andExpect(status().isNotFound())
    }

    def "GET /v1/expenses - should require authentication"() {
        when:
        def result = mockMvc.perform(get("/v1/expenses"))

        then:
        result.andExpect(status().isUnauthorized())
    }
}

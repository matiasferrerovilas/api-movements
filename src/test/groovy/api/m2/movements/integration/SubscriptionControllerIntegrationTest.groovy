package api.m2.movements.integration

import api.m2.movements.entities.Subscription
import api.m2.movements.repositories.SubscriptionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

import java.time.LocalDate

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class SubscriptionControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Autowired
    SubscriptionRepository subscriptionRepository

    def "GET /v1/subscriptions - should return subscriptions"() {
        given:
        subscriptionRepository.save(Subscription.builder()
                .description("Netflix")
                .amount(new BigDecimal("15.99"))
                .currency(testCurrency)
                .workspace(testWorkspace)
                .owner(testUser)
                .lastPayment(LocalDate.now())
                .build())

        when:
        def result = mockMvc.perform(get("/v1/subscriptions")
                .with(jwtAuth()))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$').isArray())
                .andExpect(jsonPath('$.length()').value(1))
                .andExpect(jsonPath('$[0].description').value("Netflix"))
                .andExpect(jsonPath('$[0].amount').value(15.99))
    }

    def "GET /v1/subscriptions - should return empty list when no subscriptions"() {
        when:
        def result = mockMvc.perform(get("/v1/subscriptions")
                .with(jwtAuth()))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$').isArray())
                .andExpect(jsonPath('$.length()').value(0))
    }

    def "GET /v1/subscriptions - should filter by currency"() {
        given:
        def usdCurrency = getOrCreateCurrency("USD", "US Dollar")

        subscriptionRepository.save(Subscription.builder()
                .description("Netflix")
                .amount(new BigDecimal("15.99"))
                .currency(testCurrency)
                .workspace(testWorkspace)
                .owner(testUser)
                .build())

        subscriptionRepository.save(Subscription.builder()
                .description("Spotify")
                .amount(new BigDecimal("9.99"))
                .currency(usdCurrency)
                .workspace(testWorkspace)
                .owner(testUser)
                .build())

        when:
        def result = mockMvc.perform(get("/v1/subscriptions")
                .with(jwtAuth())
                .param("currencySymbol", "USD"))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$').isArray())
                .andExpect(jsonPath('$.length()').value(1))
                .andExpect(jsonPath('$[0].description').value("Spotify"))
    }

    def "POST /v1/subscriptions - should create subscription and return 201"() {
        given:
        def request = [
                description: "HBO Max",
                amount     : 12.99,
                currency   : [symbol: "ARS", id: testCurrency.id],
                isPaid     : false
        ]

        when:
        def result = mockMvc.perform(post("/v1/subscriptions")
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isCreated())

        and:
        def subscriptions = subscriptionRepository.findAll()
        subscriptions.any { it.description == "HBO Max" }
    }

    def "POST /v1/subscriptions - should return 400 for missing required fields"() {
        given:
        def request = [
                description: "", // Empty description
                amount     : null,
                currency   : [symbol: "ARS"]
        ]

        when:
        def result = mockMvc.perform(post("/v1/subscriptions")
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isBadRequest())
    }

    def "PATCH /v1/subscriptions/{id}/payment - should register payment"() {
        given:
        def category = getOrCreateCategory("SERVICIOS")
        def subscription = subscriptionRepository.save(Subscription.builder()
                .description("Netflix")
                .amount(new BigDecimal("15.99"))
                .currency(testCurrency)
                .workspace(testWorkspace)
                .owner(testUser)
                .lastPayment(LocalDate.now().minusMonths(1))
                .build())

        when:
        def result = mockMvc.perform(patch("/v1/subscriptions/{id}/payment", subscription.id)
                .with(jwtAuth()))

        then:
        result.andExpect(status().isOk())

        and:
        def updated = subscriptionRepository.findById(subscription.id).get()
        updated.lastPayment.month == LocalDate.now().month
    }

    def "PATCH /v1/subscriptions/{id} - should update subscription"() {
        given:
        def subscription = subscriptionRepository.save(Subscription.builder()
                .description("Netflix")
                .amount(new BigDecimal("15.99"))
                .currency(testCurrency)
                .workspace(testWorkspace)
                .owner(testUser)
                .build())

        def updateRequest = [
                description: "Netflix Premium",
                amount     : 19.99
        ]

        when:
        def result = mockMvc.perform(patch("/v1/subscriptions/{id}", subscription.id)
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))

        then:
        result.andExpect(status().isOk())

        and:
        def updated = subscriptionRepository.findById(subscription.id).get()
        updated.description == "Netflix Premium"
        updated.amount == new BigDecimal("19.99")
    }

    def "DELETE /v1/subscriptions/{id} - should delete subscription and return 204"() {
        given:
        def subscription = subscriptionRepository.save(Subscription.builder()
                .description("Netflix")
                .amount(new BigDecimal("15.99"))
                .currency(testCurrency)
                .workspace(testWorkspace)
                .owner(testUser)
                .build())

        when:
        def result = mockMvc.perform(delete("/v1/subscriptions/{id}", subscription.id)
                .with(jwtAuth()))

        then:
        result.andExpect(status().isNoContent())

        and:
        !subscriptionRepository.findById(subscription.id).isPresent()
    }

    def "GET /v1/subscriptions - should require authentication"() {
        when:
        def result = mockMvc.perform(get("/v1/subscriptions"))

        then:
        result.andExpect(status().isUnauthorized())
    }
}

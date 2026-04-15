package api.m2.movements.integration

import api.m2.movements.entities.User
import api.m2.movements.enums.UserType
import org.springframework.http.MediaType

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class OnboardingControllerIntegrationTest extends BaseControllerIntegrationTest {

    def "POST /v1/onboarding - should complete onboarding and return 204"() {
        given:
        // Use unique email that doesn't exist yet - onboarding creates the user from JWT
        def uniqueEmail = "onboarding-${UUID.randomUUID()}@test.com"

        def request = [
                userType       : "PERSONAL",
                accountsToAdd  : ["Gastos personales", "Ahorros"],
                categoriesToAdd: ["COMIDA", "TRANSPORTE"],
                banksToAdd     : [
                        [description: "GALICIA", isDefault: true],
                        [description: "BBVA", isDefault: false]
                ],
                onBoardingAmount: [
                        amount      : 50000.00,
                        accountToAdd: "Cuenta principal",
                        bank        : "GALICIA",
                        currency    : "ARS"
                ]
        ]

        when:
        def result = mockMvc.perform(post("/v1/onboarding")
                .with(jwtAuth(uniqueEmail, UUID.randomUUID().toString(), ["ROLE_ADMIN"]))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isNoContent())

        and: "User should be created and updated"
        def createdUser = userRepository.findByEmail(uniqueEmail).get()
        createdUser.isFirstLogin == false
        createdUser.userType == UserType.PERSONAL
    }

    def "POST /v1/onboarding - should complete onboarding without income"() {
        given:
        def uniqueEmail = "no-income-${UUID.randomUUID()}@test.com"

        def request = [
                userType       : "ENTERPRISE",
                accountsToAdd  : ["Cuenta empresarial"],
                categoriesToAdd: [],
                banksToAdd     : [
                        [description: "SANTANDER", isDefault: true]
                ],
                onBoardingAmount: null
        ]

        when:
        def result = mockMvc.perform(post("/v1/onboarding")
                .with(jwtAuth(uniqueEmail, UUID.randomUUID().toString(), ["ROLE_ADMIN"]))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isNoContent())

        and:
        def createdUser = userRepository.findByEmail(uniqueEmail).get()
        createdUser.userType == UserType.ENTERPRISE
    }

    def "POST /v1/onboarding - should return 400 for invalid request"() {
        given:
        def uniqueEmail = "invalid-${UUID.randomUUID()}@test.com"

        def request = [
                userType       : null, // Required field
                accountsToAdd  : null, // Required field
                categoriesToAdd: null, // Required field
                banksToAdd     : null  // Required field
        ]

        when:
        def result = mockMvc.perform(post("/v1/onboarding")
                .with(jwtAuth(uniqueEmail, UUID.randomUUID().toString(), ["ROLE_ADMIN"]))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isBadRequest())
    }

    def "POST /v1/onboarding - should return 400 for blank userType"() {
        given:
        def uniqueEmail = "blank-${UUID.randomUUID()}@test.com"

        def request = [
                userType       : "",
                accountsToAdd  : [],
                categoriesToAdd: [],
                banksToAdd     : []
        ]

        when:
        def result = mockMvc.perform(post("/v1/onboarding")
                .with(jwtAuth(uniqueEmail, UUID.randomUUID().toString(), ["ROLE_ADMIN"]))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isBadRequest())
    }

    def "POST /v1/onboarding - should require authentication"() {
        given:
        def request = [
                userType       : "PERSONAL",
                accountsToAdd  : [],
                categoriesToAdd: [],
                banksToAdd     : []
        ]

        when:
        def result = mockMvc.perform(post("/v1/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isUnauthorized())
    }

    def "POST /v1/onboarding - should complete with empty lists"() {
        given:
        def uniqueEmail = "minimal-${UUID.randomUUID()}@test.com"

        def request = [
                userType       : "PERSONAL",
                accountsToAdd  : [],
                categoriesToAdd: [],
                banksToAdd     : [],
                onBoardingAmount: null
        ]

        when:
        def result = mockMvc.perform(post("/v1/onboarding")
                .with(jwtAuth(uniqueEmail, UUID.randomUUID().toString(), ["ROLE_ADMIN"]))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isNoContent())
    }
}

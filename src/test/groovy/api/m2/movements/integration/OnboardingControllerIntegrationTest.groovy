package api.m2.movements.integration

import org.springframework.http.MediaType

import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class OnboardingControllerIntegrationTest extends BaseControllerIntegrationTest {

    def "POST /v1/onboarding - should complete onboarding and return 204"() {
        given:
        // Use unique email that doesn't exist yet - onboarding creates the user from JWT
        def uniqueEmail = "onboarding-${UUID.randomUUID()}@test.com"

        def request = [
                userType       : "PERSONAL",
                workspacesToAdd: [
                        [name: "Gastos personales", isDefault: false],
                        [name: "Ahorros", isDefault: false]
                ],
                categoriesToAdd: ["COMIDA", "TRANSPORTE"],
                banksToAdd     : [
                        [description: "GALICIA", isDefault: true],
                        [description: "BBVA", isDefault: false]
                ],
                currenciesToAdd: [
                        [symbol: "ARS", description: "Peso argentino"],
                        [symbol: "USD", description: "Dólar"]
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

        and: "Onboarding should have created the user+workspaces atomically and flipped first-login via IdentityClient"
        identityMock.verify(postRequestedFor(urlPathEqualTo("/v1/onboarding/start")))
        identityMock.verify(patchRequestedFor(urlPathEqualTo("/v1/onboarding/${testUserId}/first-login")))
    }

    def "POST /v1/onboarding - should complete onboarding without income"() {
        given:
        def uniqueEmail = "no-income-${UUID.randomUUID()}@test.com"

        def request = [
                userType       : "ENTERPRISE",
                workspacesToAdd: [
                        [name: "Cuenta empresarial", isDefault: false]
                ],
                categoriesToAdd: [],
                banksToAdd     : [
                        [description: "SANTANDER", isDefault: true]
                ],
                currenciesToAdd: [],
                onBoardingAmount: null
        ]

        when:
        def result = mockMvc.perform(post("/v1/onboarding")
                .with(jwtAuth(uniqueEmail, UUID.randomUUID().toString(), ["ROLE_ADMIN"]))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isNoContent())

        and: "Onboarding should have created the user via IdentityClient"
        identityMock.verify(postRequestedFor(urlPathEqualTo("/v1/onboarding/start")))
    }

    def "POST /v1/onboarding - should return 400 for invalid request"() {
        given:
        def uniqueEmail = "invalid-${UUID.randomUUID()}@test.com"

        def request = [
                userType       : null, // Required field
                workspacesToAdd: null, // Required field
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
                workspacesToAdd: [],
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
                workspacesToAdd: [],
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
                workspacesToAdd: [],
                categoriesToAdd: [],
                banksToAdd     : [],
                currenciesToAdd: [],
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

package api.m2.movements.integration

import groovy.json.JsonOutput

import java.time.LocalDate

import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.matching
import static com.github.tomakehurst.wiremock.client.WireMock.okJson
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get as mockGet
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Cubre el gap de "READ_ONLY es un gate de UI, no de backend" del roadmap: hasta esta ronda,
 * WorkspaceQueryService.verifyCanWrite no existía y ningún endpoint de creación validaba rol,
 * solo membership — un miembro READ_ONLY podía crear/editar todo igual llamando a la API
 * directo. Pisa el stub de /v1/users/me?workspaceId= del caller para devolver READ_ONLY y
 * confirma que las rutas de escritura ahora se bloquean, mientras las de lectura siguen
 * funcionando (READ_ONLY es "solo lectura", no "sin acceso").
 */
class ReadOnlyWriteProtectionIntegrationTest extends BaseControllerIntegrationTest {

    def setup() {
        stubFor(get(urlPathEqualTo("/v1/users/me"))
                .withQueryParam("workspaceId", matching("\\d+"))
                .willReturn(okJson(JsonOutput.toJson([
                        id        : testUserId,
                        email     : TEST_USER_EMAIL,
                        givenName : "Integration",
                        familyName: "Test",
                        userType  : "PERSONAL",
                        metadata  : [isFirstLogin: false, hasSeenTour: true, userRole: [], workspaceRole: "READ_ONLY"]
                ]))))
    }

    def "POST /v1/expenses - a READ_ONLY member is rejected with 403"() {
        given:
        getOrCreateCategory("COMIDA")
        def request = [
                amount     : 250.50,
                date       : LocalDate.now().toString(),
                description: "Almuerzo",
                categories : [[description: "COMIDA"]],
                type       : "DEBITO",
                currency   : "ARS",
                cuotaActual: 0,
                cuotaTotal : 0,
        ]

        when:
        def result = mockMvc.perform(post("/v1/expenses")
                .with(jwtAuth())
                .contentType("application/json")
                .content(JsonOutput.toJson(request)))

        then:
        result.andExpect(status().isForbidden())
    }

    def "POST /v1/budgets - a READ_ONLY member is rejected with 403"() {
        given:
        def request = [category: "COMIDA", currency: "ARS", amount: 5000.00]

        when:
        def result = mockMvc.perform(post("/v1/budgets")
                .with(jwtAuth())
                .contentType("application/json")
                .content(JsonOutput.toJson(request)))

        then:
        result.andExpect(status().isForbidden())
    }

    def "GET /v1/expenses - a READ_ONLY member can still read"() {
        when:
        def result = mockMvc.perform(mockGet("/v1/expenses")
                .with(jwtAuth())
                .param("page", "0")
                .param("size", "10"))

        then:
        result.andExpect(status().isOk())
    }
}

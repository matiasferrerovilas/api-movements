package api.m2.movements.integration

import groovy.json.JsonOutput

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.get as wmGet
import static com.github.tomakehurst.wiremock.client.WireMock.okJson
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserControllerIntegrationTest extends BaseControllerIntegrationTest {

    def "GET /v1/users/me - enriches the response with the caller's role in their default workspace"() {
        given:
        // El default de BaseControllerIntegrationTest.setup() ya configura testWorkspaceId como
        // DEFAULT_WORKSPACE local — este stub pisa /v1/users/me específicamente cuando viene con
        // ese workspaceId, simulando la segunda llamada (enriquecida) que hace el controller.
        stubFor(wmGet(urlPathEqualTo("/v1/users/me"))
                .withQueryParam("workspaceId", equalTo(testWorkspaceId.toString()))
                .willReturn(okJson(JsonOutput.toJson([
                        id        : testUserId,
                        email     : TEST_USER_EMAIL,
                        givenName : "Integration",
                        familyName: "Test",
                        userType  : "PERSONAL",
                        metadata  : [isFirstLogin: false, hasSeenTour: true, userRole: [], workspaceRole: "OWNER"]
                ]))))

        when:
        def result = mockMvc.perform(get("/v1/users/me").with(jwtAuth()))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.metadata.workspaceRole').value("OWNER"))
    }

    def "GET /v1/users/me - returns the plain response when the user has no default workspace"() {
        given:
        userSettingRepository.deleteAll()

        when:
        def result = mockMvc.perform(get("/v1/users/me").with(jwtAuth()))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.metadata.workspaceRole').doesNotExist())
    }
}

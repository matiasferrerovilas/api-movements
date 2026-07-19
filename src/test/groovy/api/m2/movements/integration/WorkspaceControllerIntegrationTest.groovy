package api.m2.movements.integration

import groovy.json.JsonOutput
import org.springframework.http.MediaType

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.delete as wmDelete
import static com.github.tomakehurst.wiremock.client.WireMock.okJson
import static com.github.tomakehurst.wiremock.client.WireMock.post as wmPost
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class WorkspaceControllerIntegrationTest extends BaseControllerIntegrationTest {

    def "POST /v1/workspace - should create workspace via IdentityClient and return 201"() {
        given:
        def request = [description: "Mi nuevo workspace"]

        stubFor(wmPost(urlPathEqualTo("/v1/workspaces"))
                .willReturn(okJson(JsonOutput.toJson([[id: 50, description: "Mi nuevo workspace"]]))))

        when:
        def result = mockMvc.perform(post("/v1/workspace")
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isCreated())
    }

    def "POST /v1/workspace - should return 400 for blank description"() {
        given:
        def request = [description: ""]

        when:
        def result = mockMvc.perform(post("/v1/workspace")
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isBadRequest())
    }

    def "DELETE /v1/workspace/{workspaceId} - should leave workspace via IdentityClient"() {
        given:
        stubFor(wmDelete(urlPathEqualTo("/v1/workspaces/${testWorkspaceId}"))
                .willReturn(aResponse().withStatus(204)))

        when:
        def result = mockMvc.perform(delete("/v1/workspace/{workspaceId}", testWorkspaceId)
                .with(jwtAuth()))

        then:
        result.andExpect(status().isNoContent())
    }
}

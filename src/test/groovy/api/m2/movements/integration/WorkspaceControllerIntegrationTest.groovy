package api.m2.movements.integration

import groovy.json.JsonOutput
import org.springframework.http.MediaType

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class WorkspaceControllerIntegrationTest extends BaseControllerIntegrationTest {

    def "POST /v1/workspace - should create workspace via IdentityClient and return 201"() {
        given:
        def request = [description: "Mi nuevo workspace"]

        stubFor(post(urlPathEqualTo("/v1/users/${testUserId}/workspaces"))
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

    def "GET /v1/workspace/count - should return workspaces with member count"() {
        given:
        stubFor(get(urlPathEqualTo("/v1/users/${testUserId}/workspaces"))
                .willReturn(okJson(JsonOutput.toJson([[id: testWorkspaceId, name: "Hogar", membersCount: 1, owner: TEST_USER_EMAIL]]))))

        when:
        def result = mockMvc.perform(get("/v1/workspace/count")
                .with(jwtAuth()))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$').isArray())
                .andExpect(jsonPath('$[0].id').value(testWorkspaceId))
    }

    def "DELETE /v1/workspace/{workspaceId} - should leave workspace via IdentityClient"() {
        given:
        stubFor(delete(urlPathEqualTo("/v1/workspaces/${testWorkspaceId}/members/${testUserId}"))
                .willReturn(aResponse().withStatus(204)))

        when:
        def result = mockMvc.perform(delete("/v1/workspace/{workspaceId}", testWorkspaceId)
                .with(jwtAuth()))

        then:
        result.andExpect(status().isNoContent())
    }

    def "PATCH /v1/workspace/{id}/default - should update default workspace"() {
        given:
        stubFor(get(urlPathEqualTo("/v1/users/${testUserId}/workspaces"))
                .willReturn(okJson(JsonOutput.toJson([[id: testWorkspaceId, name: "Hogar", membersCount: 1, owner: TEST_USER_EMAIL]]))))

        when:
        def result = mockMvc.perform(patch("/v1/workspace/{id}/default", testWorkspaceId)
                .with(jwtAuth()))

        then:
        result.andExpect(status().isOk())
    }

    def "GET /v1/workspace/count - should require authentication"() {
        when:
        def result = mockMvc.perform(get("/v1/workspace/count"))

        then:
        result.andExpect(status().isUnauthorized())
    }
}

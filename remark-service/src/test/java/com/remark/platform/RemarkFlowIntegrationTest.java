package com.remark.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remark.platform.client.NotificationClient;
import com.remark.platform.client.ReportClient;
import com.remark.platform.client.UserProfileResponse;
import com.remark.platform.client.UserServiceClient;
import com.remark.platform.security.RoleCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class RemarkFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("remark_platform")
            .withUsername("remark")
            .withPassword("remark");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private NotificationClient notificationClient;

    @MockBean
    private ReportClient reportClient;

    @Test
    void fullRemarkLifecycle() throws Exception {
        UUID branchId = UUID.randomUUID();
        UUID applicantId = UUID.randomUUID();
        UUID branchHeadId = UUID.randomUUID();
        UUID masterId = UUID.randomUUID();

        doNothing().when(userServiceClient).ensureActiveMasterInBranch(eq(masterId), eq(branchId));
        when(userServiceClient.getActiveUser(applicantId)).thenReturn(
                new UserProfileResponse(
                        applicantId,
                        "applicant@remark.local",
                        "Applicant User",
                        "Engineer",
                        branchId,
                        "ACTIVE",
                        Set.of(RoleCode.APPLICANT)
                )
        );

        String createBody = """
                {
                  "objectBuilding": "Building A",
                  "description": "Wall crack",
                  "discoveredAt": "%s"
                }
                """.formatted(LocalDate.now());

        MvcResult createResult = mockMvc.perform(post("/api/v1/remarks")
                        .header("X-User-Id", applicantId)
                        .header("X-Branch-Id", branchId)
                        .header("X-User-Roles", "APPLICANT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID remarkId = UUID.fromString(created.get("id").asText());

        mockMvc.perform(patch("/api/v1/remarks/{id}/assign", remarkId)
                        .header("X-User-Id", branchHeadId)
                        .header("X-Branch-Id", branchId)
                        .header("X-User-Roles", "BRANCH_HEAD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"masterId\":\"" + masterId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        mockMvc.perform(patch("/api/v1/remarks/{id}/start", remarkId)
                        .header("X-User-Id", masterId)
                        .header("X-Branch-Id", branchId)
                        .header("X-User-Roles", "MASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/v1/remarks/{id}/resolve", remarkId)
                        .header("X-User-Id", masterId)
                        .header("X-Branch-Id", branchId)
                        .header("X-User-Roles", "MASTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionMessage\":\"Fixed the crack\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(patch("/api/v1/remarks/{id}/close", remarkId)
                        .header("X-User-Id", branchHeadId)
                        .header("X-Branch-Id", branchId)
                        .header("X-User-Roles", "BRANCH_HEAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/remarks/{id}", remarkId)
                        .header("X-User-Id", branchHeadId)
                        .header("X-Branch-Id", branchId)
                        .header("X-User-Roles", "BRANCH_HEAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.resolutionMessage").value("Fixed the crack"));
    }
}

package com.remark.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remark.platform.client.UserProfileResponse;
import com.remark.platform.client.UserServiceClient;
import com.remark.platform.security.RoleCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
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
class RemarkFlowIntegrationTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Minsk");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> env("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/remark_platform"));
        registry.add("spring.datasource.username", () -> env("SPRING_DATASOURCE_USERNAME", "remark"));
        registry.add("spring.datasource.password", () -> env("SPRING_DATASOURCE_PASSWORD", "remark"));
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeAll
    static void postgresAvailable() {
        String url = env("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/remark_platform");
        String username = env("SPRING_DATASOURCE_USERNAME", "remark");
        String password = env("SPRING_DATASOURCE_PASSWORD", "remark");
        try (var connection = DriverManager.getConnection(url, username, password)) {
            Assumptions.assumeTrue(connection.isValid(2), "PostgreSQL connection is not valid");
        } catch (Exception ex) {
            Assumptions.assumeTrue(false, "PostgreSQL is not available for integration tests: " + ex.getMessage());
        }
    }

    @Test
    void fullRemarkLifecycle() throws Exception {
        UUID branchId = UUID.randomUUID();
        UUID applicantId = UUID.randomUUID();
        UUID branchHeadId = UUID.randomUUID();
        UUID masterId = UUID.randomUUID();
        LocalDateTime estimatedResolutionAt = LocalDateTime.now(BUSINESS_ZONE).plusDays(3);

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

        mockMvc.perform(patch("/api/v1/remarks/{id}/estimated-resolution", remarkId)
                        .header("X-User-Id", masterId)
                        .header("X-Branch-Id", branchId)
                        .header("X-User-Roles", "MASTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estimatedResolutionAt": "%s"
                                }
                                """.formatted(estimatedResolutionAt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.estimatedResolutionDuration").value(3))
                .andExpect(jsonPath("$.estimatedResolutionUnit").value("DAYS"))
                .andExpect(jsonPath("$.estimatedResolutionAt").value(estimatedResolutionAt.toString()));

        MvcResult calendarResult = mockMvc.perform(get("/api/v1/remarks/calendar")
                        .header("X-User-Id", masterId)
                        .header("X-Branch-Id", branchId)
                        .header("X-User-Roles", "MASTER")
                        .param("from", estimatedResolutionAt.minusDays(1).toString())
                        .param("to", estimatedResolutionAt.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode calendarEvents = objectMapper.readTree(calendarResult.getResponse().getContentAsString());
        assertThat(calendarEvents.isArray()).isTrue();
        assertThat(calendarEvents.size()).isGreaterThanOrEqualTo(1);

        JsonNode calendarEvent = StreamSupport.stream(calendarEvents.spliterator(), false)
                .filter(event -> remarkId.toString().equals(event.path("remarkId").asText()))
                .findFirst()
                .orElseThrow();

        assertThat(calendarEvent.get("remarkNumber").asText()).isEqualTo(created.get("remarkNumber").asText());
        assertThat(calendarEvent.get("objectBuilding").asText()).isEqualTo("Building A");
        assertThat(calendarEvent.get("status").asText()).isEqualTo("ASSIGNED");
        assertThat(calendarEvent.get("estimatedResolutionAt").asText())
                .isEqualTo(estimatedResolutionAt.toString());

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

        mockMvc.perform(get("/api/v1/remarks/{id}", remarkId)
                        .header("X-User-Id", branchHeadId)
                        .header("X-Branch-Id", branchId)
                        .header("X-User-Roles", "BRANCH_HEAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionMessage").value("Fixed the crack"));
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}

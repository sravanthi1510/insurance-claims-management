package com.chubb.claims.claim.api;

import com.chubb.claims.claim.domain.ClaimStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void submissionAndReviewAreAvailableThroughTheApi() throws Exception {
        String body = """
                {
                  "claimantName": "Jamie Claimant",
                  "claimantEmail": "jamie@example.com",
                  "market": "AU",
                  "incidentDate": "2026-01-10",
                  "incidentDescription": "Vehicle damage",
                  "estimatedLoss": 1250.00
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/claims/submit")
                        .with(user("agent@example.com").roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status", is(ClaimStatus.SUBMITTED.name())))
                .andReturn();

        String id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.claimId");
        assertTrue(id.matches("[0-9A-F]{16}"));
        String claimReference = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.claimReference");

        MvcResult secondResult = mockMvc.perform(post("/api/v1/claims/submit")
                        .with(user("agent@example.com").roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claimId").value(matchesPattern("[0-9A-F]{16}")))
                .andReturn();
        String secondClaimId = com.jayway.jsonpath.JsonPath.read(
                secondResult.getResponse().getContentAsString(), "$.claimId");
        assertNotEquals(id, secondClaimId);

        mockMvc.perform(post("/api/v1/claims/{id}/review", id)
                        .with(user("Alex Officer").roles("AGENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(ClaimStatus.UNDER_REVIEW.name())));

        mockMvc.perform(get("/api/v1/claims/{id}", id)
                        .with(user("Alex Officer").roles("AGENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedOfficer", is("Alex Officer")));

        mockMvc.perform(get("/api/v1/claims/reference/{reference}", claimReference)
                        .with(user("Alex Officer").roles("AGENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(ClaimStatus.UNDER_REVIEW.name())))
                .andExpect(jsonPath("$.claimantEmail").doesNotExist());
    }
}

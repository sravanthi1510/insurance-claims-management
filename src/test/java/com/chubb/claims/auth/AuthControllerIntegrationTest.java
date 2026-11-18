package com.chubb.claims.auth;

import com.chubb.claims.auth.entity.AgentAccount;
import com.chubb.claims.auth.repository.AgentAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.auth.agent-registration-code=test-agent-invite")
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentAccountRepository agentAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void agentCanSignInWhenStoredEmailUsesDifferentLetterCase() throws Exception {
        String email = UUID.randomUUID() + "@Example.com";
        agentAccountRepository.save(new AgentAccount(
                email,
                "Case-Insensitive Agent",
                passwordEncoder.encode("password-123")
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "password-123", "role": "AGENT"}
                                """.formatted(email.toLowerCase(java.util.Locale.ROOT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("AGENT")))
                .andExpect(jsonPath("$.fullName", is("Case-Insensitive Agent")));
    }

    @Test
    void homePageIsPublicButClaimApiStillRequiresLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Create account")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("window.location.assign")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[hidden]{display:none!important}")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("inviteRow.hidden = !(registering && role === \"AGENT\")")));

        mockMvc.perform(get("/api/v1/claims"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerAndAgentAccountsUseSeparateStoresAndRoleProtectedRoutes() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String agentEmail = UUID.randomUUID() + "@example.com";
        String password = "password-123";
        String customerBody = """
                {
                  "fullName": "Portal Customer",
                  "email": "%s",
                  "password": "%s",
                  "role": "CUSTOMER"
                }
                """.formatted(email, password);
        String agentBody = """
                {
                  "fullName": "Portal Agent",
                  "email": "%s",
                  "password": "%s",
                  "role": "AGENT",
                  "agentInviteCode": "test-agent-invite"
                }
                """.formatted(agentEmail, password);

        MvcResult customerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("CUSTOMER")))
                .andReturn();

        mockMvc.perform(get("/portal.html")
                        .session((MockHttpSession) customerResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("http://localhost:5173/")));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agentBody.replace(agentEmail, email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is(
                        "An account with this email already exists. Please sign in instead.")));

        MvcResult agentResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("AGENT")))
                .andReturn();

        mockMvc.perform(get("/api/v1/claims/agent/queue")
                        .session((MockHttpSession) customerResult.getRequest().getSession(false)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s", "role": "CUSTOMER"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("CUSTOMER")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s", "role": "AGENT"}
                                """.formatted(agentEmail, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("AGENT")));

        mockMvc.perform(get("/api/v1/claims/agent/queue")
                        .session((MockHttpSession) agentResult.getRequest().getSession(false)))
                .andExpect(status().isOk());
    }

    @Test
    void agentRegistrationRequiresInvitationCode() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Uninvited Agent",
                                  "email": "uninvited@example.com",
                                  "password": "password-123",
                                  "role": "AGENT"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registeringWithAnExistingCustomerEmailPromptsTheUserToSignIn() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String registrationBody = """
                {
                  "fullName": "Existing Customer",
                  "email": "%s",
                  "password": "password-123",
                  "role": "CUSTOMER"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody.replace(email, email.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is(
                        "An account with this email already exists. Please sign in instead.")));
    }

    @Test
    void customerCanProvideRequestedInformationWithAuthenticatedSession() throws Exception {
        String customerEmail = UUID.randomUUID() + "@example.com";
        MvcResult customerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Information Customer",
                                  "email": "%s",
                                  "password": "password-123",
                                  "role": "CUSTOMER"
                                }
                                """.formatted(customerEmail)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult claimResult = mockMvc.perform(post("/api/v1/claims/submit")
                        .session((MockHttpSession) customerResult.getRequest().getSession(false))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "claimantName": "Information Customer",
                                  "claimantEmail": "%s",
                                  "market": "AU",
                                  "incidentDate": "2026-01-15",
                                  "incidentDescription": "Hospital admission following an incident",
                                  "estimatedLoss": 1250.00
                                }
                                """.formatted(customerEmail)))
                .andExpect(status().isCreated())
                .andReturn();
        String claimId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(claimResult.getResponse().getContentAsString())
                .get("claimId")
                .asText();

        MvcResult agentResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Review Agent",
                                  "email": "%s",
                                  "password": "password-123",
                                  "role": "AGENT",
                                  "agentInviteCode": "test-agent-invite"
                                }
                                """.formatted(UUID.randomUUID() + "@example.com")))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession agentSession = (MockHttpSession) agentResult.getRequest().getSession(false);

        mockMvc.perform(post("/api/v1/claims/{id}/review", claimId)
                        .session(agentSession))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/claims/{id}/information-requests", claimId)
                        .session(agentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Please provide hospital admission details."}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/claims/{id}/additional-information", claimId)
                        .session((MockHttpSession) customerResult.getRequest().getSession(false))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"information": "Admitted in hospital"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UNDER_REVIEW")))
                .andExpect(jsonPath("$.additionalInformation", is("Admitted in hospital")));
    }
}

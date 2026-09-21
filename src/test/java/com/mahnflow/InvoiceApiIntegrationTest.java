package com.mahnflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// full flow test through the http layer, against real postgres + redis.
// i use @WithMockUser so i don't have to mint a real jwt for every test — it just
// fakes the logged-in user with a role. good enough to check the @PreAuthorize
// rules and the happy path.
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class InvoiceApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String body() {
        return """
                {
                  "customerId": "%s",
                  "amount": 250.00,
                  "currency": "EUR",
                  "dueDate": "2030-01-01"
                }
                """.formatted(UUID.randomUUID());
    }

    private String createInvoice() throws Exception {
        MvcResult result = mvc.perform(post("/api/invoices")
                        .contentType(APPLICATION_JSON).content(body()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        return idOf(result);
    }

    @Test
    void create_issue_pay_flow_works() throws Exception {
        String id = createInvoice();

        mvc.perform(post("/api/invoices/" + id + "/issue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"));

        mvc.perform(post("/api/invoices/" + id + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void paying_a_draft_is_rejected_with_conflict() throws Exception {
        String id = createInvoice();

        // draft can not be paid, state machine blocks it -> 409
        mvc.perform(post("/api/invoices/" + id + "/pay"))
                .andExpect(status().isConflict());
    }

    @Test
    void same_idempotency_key_returns_the_same_invoice() throws Exception {
        String key = UUID.randomUUID().toString();
        String payload = body();

        MvcResult first = mvc.perform(post("/api/invoices")
                        .header("Idempotency-Key", key)
                        .contentType(APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn();

        MvcResult second = mvc.perform(post("/api/invoices")
                        .header("Idempotency-Key", key)
                        .contentType(APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn();

        // same key -> we must get back the very same invoice, not a new one
        assertThat(idOf(first)).isEqualTo(idOf(second));
    }

    private String idOf(MvcResult result) throws Exception {
        JsonNode node = json.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }
}

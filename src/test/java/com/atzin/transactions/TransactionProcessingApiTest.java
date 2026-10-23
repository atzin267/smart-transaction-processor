package com.atzin.transactions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionProcessingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void eventoValido_devuelveApplied_yElGetMuestraElEstado() throws Exception {
        String body = """
                {"eventId":"EVT-API-1","transactionId":"TXN-API-1","type":"STATUS_UPDATED","status":"CREATED","timestamp":"2026-10-21T14:30:00Z"}
                """;

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("APPLIED"))
                .andExpect(jsonPath("$.eventId").value("EVT-API-1"))
                .andExpect(jsonPath("$.transactionId").value("TXN-API-1"));

        mockMvc.perform(get("/transactions/TXN-API-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.lastEventId").value("EVT-API-1"));
    }

    @Test
    void faltaTransactionId_devuelve400ConElCampoEnLosDetalles() throws Exception {
        String body = """
                {"eventId":"EVT-API-2","type":"STATUS_UPDATED","status":"CREATED","timestamp":"2026-10-21T14:30:00Z"}
                """;

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[?(@.field=='transactionId')]").exists());
    }

    @Test
    void statusDesconocido_devuelve400Malformed() throws Exception {
        String body = """
                {"eventId":"EVT-API-3","transactionId":"TXN-API-3","type":"STATUS_UPDATED","status":"NO_EXISTE","timestamp":"2026-10-21T14:30:00Z"}
                """;

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void jsonMalFormado_devuelve400Malformed() throws Exception {
        String body = "{ \"eventId\": \"EVT-API-4\", esto no es JSON válido";

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void getDeTransaccionInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/transactions/TXN-NO-EXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void mismoEventoDosVeces_devuelveDuplicateYElEstadoNoCambia() throws Exception {
        String body = """
                {"eventId":"EVT-API-5","transactionId":"TXN-API-5","type":"STATUS_UPDATED","status":"CREATED","timestamp":"2026-10-21T14:30:00Z"}
                """;

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("APPLIED"));

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("DUPLICATE"));

        mockMvc.perform(get("/transactions/TXN-API-5"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.lastEventId").value("EVT-API-5"));
    }

    @Test
    void eventoMasAntiguo_devuelveStaleYElEstadoNoCambia() throws Exception {
        String creado = """
                {"eventId":"EVT-API-5B","transactionId":"TXN-API-6","type":"STATUS_UPDATED","status":"CREATED","timestamp":"2026-10-21T09:00:00Z"}
                """;
        String actual = """
                {"eventId":"EVT-API-6","transactionId":"TXN-API-6","type":"STATUS_UPDATED","status":"PROCESSING","timestamp":"2026-10-21T14:30:00Z"}
                """;
        String viejo = """
                {"eventId":"EVT-API-7","transactionId":"TXN-API-6","type":"STATUS_UPDATED","status":"COMPLETED","timestamp":"2026-10-21T10:00:00Z"}
                """;

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(creado))
                .andExpect(status().isOk());

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(actual))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("APPLIED"));

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(viejo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("STALE"));

        mockMvc.perform(get("/transactions/TXN-API-6"))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.lastEventId").value("EVT-API-6"));
    }

    @Test
    void transicionInvalida_devuelve409_yUnaSolicitudValidaSiguienteSigueFuncionando() throws Exception {
        String creado = """
                {"eventId":"EVT-API-7B","transactionId":"TXN-API-7","type":"STATUS_UPDATED","status":"CREATED","timestamp":"2026-10-21T14:00:00Z"}
                """;
        String completado = """
                {"eventId":"EVT-API-8","transactionId":"TXN-API-7","type":"STATUS_UPDATED","status":"COMPLETED","timestamp":"2026-10-21T14:30:00Z"}
                """;
        String vuelveAProcessing = """
                {"eventId":"EVT-API-9","transactionId":"TXN-API-7","type":"STATUS_UPDATED","status":"PROCESSING","timestamp":"2026-10-21T15:00:00Z"}
                """;
        String otraTransaccionValida = """
                {"eventId":"EVT-API-10","transactionId":"TXN-API-8","type":"STATUS_UPDATED","status":"CREATED","timestamp":"2026-10-21T15:00:00Z"}
                """;

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(creado))
                .andExpect(status().isOk());

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(completado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("APPLIED"));

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(vuelveAProcessing))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_TRANSITION"));

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(otraTransaccionValida))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("APPLIED"));
    }

    @Test
    void primerEventoDistintoDeCreated_devuelve409YNoCreaLaTransaccion() throws Exception {
        String directoAProcessing = """
                {"eventId":"EVT-API-11","transactionId":"TXN-API-9","type":"STATUS_UPDATED","status":"PROCESSING","timestamp":"2026-10-21T14:30:00Z"}
                """;

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(directoAProcessing))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_TRANSITION"));

        mockMvc.perform(get("/transactions/TXN-API-9"))
                .andExpect(status().isNotFound());
    }
}

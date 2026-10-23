package com.atzin.transactions.service;

import com.atzin.transactions.exception.InvalidTransitionException;
import com.atzin.transactions.model.Event;
import com.atzin.transactions.model.ProcessResult;
import com.atzin.transactions.model.Status;
import com.atzin.transactions.model.Transaction;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionServiceTest {

    private final TransactionService service = new TransactionService();

    @Test
    void primerEventoSeAplicaYGuardaEstado() {
        Event event = new Event("EVT-1", "TXN-1", "STATUS_UPDATED", Status.CREATED,
                Instant.parse("2026-10-21T10:00:00Z"));

        ProcessResult result = service.process(event);

        assertThat(result).isEqualTo(ProcessResult.APPLIED);
        Transaction tx = service.findById("TXN-1").orElseThrow();
        assertThat(tx.status()).isEqualTo(Status.CREATED);
        assertThat(tx.lastEventId()).isEqualTo("EVT-1");
    }

    @Test
    void mismoEventIdDosVecesEsDuplicate() {
        Event event = new Event("EVT-2", "TXN-2", "STATUS_UPDATED", Status.CREATED,
                Instant.parse("2026-10-21T10:00:00Z"));
        service.process(event);

        ProcessResult result = service.process(event);

        assertThat(result).isEqualTo(ProcessResult.DUPLICATE);
        assertThat(service.findById("TXN-2").orElseThrow().status()).isEqualTo(Status.CREATED);
    }

    @Test
    void timestampAnteriorEsStaleYNoCambiaEstado() {
        service.process(new Event("EVT-3-0", "TXN-3", "STATUS_UPDATED", Status.CREATED,
                Instant.parse("2026-10-21T09:30:00Z")));
        Event actual = new Event("EVT-3", "TXN-3", "STATUS_UPDATED", Status.PROCESSING,
                Instant.parse("2026-10-21T10:00:00Z"));
        service.process(actual);

        Event viejo = new Event("EVT-4", "TXN-3", "STATUS_UPDATED", Status.COMPLETED,
                Instant.parse("2026-10-21T09:00:00Z"));
        ProcessResult result = service.process(viejo);

        assertThat(result).isEqualTo(ProcessResult.STALE);
        assertThat(service.findById("TXN-3").orElseThrow().status()).isEqualTo(Status.PROCESSING);
    }

    @Test
    void transicionInvalidaLanzaExcepcionYNoCambiaEstado() {
        service.process(new Event("EVT-5-0", "TXN-4", "STATUS_UPDATED", Status.CREATED,
                Instant.parse("2026-10-21T09:30:00Z")));
        Event completado = new Event("EVT-5", "TXN-4", "STATUS_UPDATED", Status.COMPLETED,
                Instant.parse("2026-10-21T10:00:00Z"));
        service.process(completado);

        Event vuelveAProcessing = new Event("EVT-6", "TXN-4", "STATUS_UPDATED", Status.PROCESSING,
                Instant.parse("2026-10-21T11:00:00Z"));

        assertThrows(InvalidTransitionException.class, () -> service.process(vuelveAProcessing));
        assertThat(service.findById("TXN-4").orElseThrow().status()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void transaccionDesconocidaDevuelveVacio() {
        assertThat(service.findById("NO-EXISTE")).isEmpty();
    }

    @Test
    void primerEventoDistintoDeCreatedLanzaExcepcionYNoCreaLaTransaccion() {
        Event directoAProcessing = new Event("EVT-7", "TXN-5", "STATUS_UPDATED", Status.PROCESSING,
                Instant.parse("2026-10-21T10:00:00Z"));

        assertThrows(InvalidTransitionException.class, () -> service.process(directoAProcessing));
        assertThat(service.findById("TXN-5")).isEmpty();
    }
}

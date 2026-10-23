package com.atzin.transactions.service;

import com.atzin.transactions.exception.InvalidTransitionException;
import com.atzin.transactions.model.Event;
import com.atzin.transactions.model.ProcessResult;
import com.atzin.transactions.model.Status;
import com.atzin.transactions.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransactionService {

    private static final Map<Status, Set<Status>> ALLOWED_TRANSITIONS = Map.of(
            Status.CREATED, Set.of(Status.CREATED, Status.PROCESSING, Status.COMPLETED, Status.FAILED),
            Status.PROCESSING, Set.of(Status.PROCESSING, Status.COMPLETED, Status.FAILED),
            Status.COMPLETED, Set.of(Status.COMPLETED),
            Status.FAILED, Set.of(Status.FAILED)
    );

    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public synchronized ProcessResult process(Event event) {
        if (processedEventIds.contains(event.eventId())) {
            return ProcessResult.DUPLICATE;
        }

        Transaction current = transactions.get(event.transactionId());

        if (current == null) {
            if (event.status() != Status.CREATED) {
                throw new InvalidTransitionException(event.transactionId(), event.status());
            }
            applyEvent(event);
            return ProcessResult.APPLIED;
        }

        // Primero el orden lógico: un evento más viejo que el último aplicado ya fue superado,
        // así que no se compara contra la máquina de estados (su transición pudo ser válida en su momento).
        if (event.timestamp().isBefore(current.lastUpdated())) {
            processedEventIds.add(event.eventId());
            return ProcessResult.STALE;
        }

        if (!ALLOWED_TRANSITIONS.get(current.status()).contains(event.status())) {
            throw new InvalidTransitionException(event.transactionId(), current.status(), event.status());
        }

        applyEvent(event);
        return ProcessResult.APPLIED;
    }

    public Optional<Transaction> findById(String transactionId) {
        return Optional.ofNullable(transactions.get(transactionId));
    }

    private void applyEvent(Event event) {
        transactions.put(event.transactionId(), new Transaction(
                event.transactionId(),
                event.status(),
                event.timestamp(),
                event.eventId()
        ));
        processedEventIds.add(event.eventId());
    }
}

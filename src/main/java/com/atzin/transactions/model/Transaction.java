package com.atzin.transactions.model;

import java.time.Instant;

public record Transaction(
        String transactionId,
        Status status,
        Instant lastUpdated,
        String lastEventId
) {
}

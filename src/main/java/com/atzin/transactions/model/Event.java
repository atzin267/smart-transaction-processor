package com.atzin.transactions.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record Event(
        @NotBlank(message = "eventId es obligatorio") String eventId,
        @NotBlank(message = "transactionId es obligatorio") String transactionId,
        @NotBlank(message = "type es obligatorio") String type,
        @NotNull(message = "status es obligatorio") Status status,
        @NotNull(message = "timestamp es obligatorio") Instant timestamp
) {
}

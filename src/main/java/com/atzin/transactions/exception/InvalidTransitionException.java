package com.atzin.transactions.exception;

import com.atzin.transactions.model.Status;

public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(String transactionId, Status currentStatus, Status requestedStatus) {
        super("No se puede pasar de %s a %s para la transacción %s"
                .formatted(currentStatus, requestedStatus, transactionId));
    }

    public InvalidTransitionException(String transactionId, Status requestedStatus) {
        super("La transacción %s no existe; el primer evento debe tener status CREATED (se recibió %s)"
                .formatted(transactionId, requestedStatus));
    }
}

package com.atzin.transactions.controller;

import com.atzin.transactions.dto.ErrorResponse;
import com.atzin.transactions.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<?> getTransaction(@PathVariable String transactionId) {
        return transactionService.findById(transactionId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("NOT_FOUND",
                                "No existe una transacción con id " + transactionId, null)));
    }
}

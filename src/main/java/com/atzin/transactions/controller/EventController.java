package com.atzin.transactions.controller;

import com.atzin.transactions.dto.EventResponse;
import com.atzin.transactions.model.Event;
import com.atzin.transactions.model.ProcessResult;
import com.atzin.transactions.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
public class EventController {

    private final TransactionService transactionService;

    public EventController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> receiveEvent(@Valid @RequestBody Event event) {
        ProcessResult result = transactionService.process(event);
        return ResponseEntity.ok(new EventResponse(result, event.eventId(), event.transactionId()));
    }
}

package com.atzin.transactions.dto;

import com.atzin.transactions.model.ProcessResult;

public record EventResponse(ProcessResult result, String eventId, String transactionId) {
}

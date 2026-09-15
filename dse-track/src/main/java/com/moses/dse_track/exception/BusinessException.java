package com.moses.dse_track.exception;

// Thrown deliberately by services for expected, user-facing error conditions
// (not found, validation failure, ownership check, etc). The message is
// always safe to return to the API client as-is — see GlobalExceptionHandler.
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

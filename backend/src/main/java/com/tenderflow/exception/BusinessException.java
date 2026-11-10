package com.tenderflow.exception;

import jakarta.ws.rs.core.Response;

public class BusinessException extends RuntimeException {
    private final Response.Status status;

    public BusinessException(String message) {
        super(message);
        this.status = Response.Status.BAD_REQUEST;
    }

    public BusinessException(String message, Response.Status status) {
        super(message);
        this.status = status;
    }

    public Response.Status getStatus() {
        return status;
    }
}
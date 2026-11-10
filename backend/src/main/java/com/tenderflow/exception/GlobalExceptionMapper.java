package com.tenderflow.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.LocalDateTime;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        int statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        String errorName = Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase();
        String message = exception.getMessage();

        if (exception instanceof BusinessException be) {
            statusCode = be.getStatus().getStatusCode();
            errorName = be.getStatus().getReasonPhrase();
        } else if (exception instanceof WebApplicationException wae) {
            statusCode = wae.getResponse().getStatus();
            Response.Status statusEnum = Response.Status.fromStatusCode(statusCode);
            errorName = statusEnum != null ? statusEnum.getReasonPhrase() : "HTTP Error";
        }

        ErrorResponse errorPayload = new ErrorResponse(
                statusCode,
                errorName,
                message != null ? message : "Si e' verificato un errore inatteso nel sistema.",
                LocalDateTime.now()
        );

        return Response.status(statusCode)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorPayload)
                .build();
    }
}
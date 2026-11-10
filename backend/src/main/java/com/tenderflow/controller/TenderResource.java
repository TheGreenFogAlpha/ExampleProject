package com.tenderflow.controller;

import com.tenderflow.model.Bid;
import com.tenderflow.model.Tender;
import io.quarkus.panache.common.Parameters;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.math.BigDecimal;
import java.util.List;

@Path("/api/tenders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Tender Management", description = "Operazioni di gestione, ricerca e aggiudicazione delle gare d'appalto")
@SecurityScheme(securitySchemeName = "jwtAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class TenderResource {

    @Channel("tender-events-out")
    Emitter<Tender> tenderEmitter;

    @GET
    @PermitAll
    @Operation(summary = "Lista gare con filtri", description = "Endpoint pubblico per la consultazione delle gare d'appalto.")
    public List<Tender> getTenders(
            @QueryParam("status") String status,
            @QueryParam("minBudget") BigDecimal minBudget,
            @QueryParam("maxBudget") BigDecimal maxBudget,
            @QueryParam("title") String title) {

        StringBuilder query = new StringBuilder("1=1");
        Parameters params = new Parameters();

        if (status != null && !status.isBlank()) {
            query.append(" and status = :status");
            params.and("status", status);
        }
        if (minBudget != null) {
            query.append(" and budget >= :minBudget");
            params.and("minBudget", minBudget);
        }
        if (maxBudget != null) {
            query.append(" and budget <= :maxBudget");
            params.and("maxBudget", maxBudget);
        }
        if (title != null && !title.isBlank()) {
            query.append(" and lower(title) like :title");
            params.and("title", "%" + title.toLowerCase() + "%");
        }

        return Tender.list(query.toString(), params);
    }

    @POST
    @RolesAllowed("BUYER")
    @Transactional
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Crea una nuova gara (Solo BUYER)", description = "Richiede ruolo BUYER.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Gara creata con successo"),
            @APIResponse(responseCode = "401", description = "Non autenticato"),
            @APIResponse(responseCode = "403", description = "Ruolo insufficiente (richiesto BUYER)")
    })
    public Response create(Tender tender) {
        if (tender.id != null) {
            tender.id = null;
        }
        tender.persist();
        tenderEmitter.send(tender);
        return Response.status(Response.Status.CREATED).entity(tender).build();
    }

    @PUT
    @Path("/{id}/close")
    @RolesAllowed("BUYER")
    @Transactional
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Chiudi gara ed aggiudica (Solo BUYER)", description = "Richiede ruolo BUYER.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Gara chiusa ed aggiudicata"),
            @APIResponse(responseCode = "401", description = "Non autenticato"),
            @APIResponse(responseCode = "403", description = "Ruolo insufficiente (richiesto BUYER)")
    })
    public Response closeTender(@Parameter(description = "ID della gara") @PathParam("id") Long id) {
        Tender tender = Tender.findById(id);
        if (tender == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Gara non trovata\"}")
                    .build();
        }

        if ("CLOSED".equalsIgnoreCase(tender.status)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"La gara e' gia' stata chiusa\"}")
                    .build();
        }

        tender.status = "CLOSED";
        tenderEmitter.send(tender);

        Bid winningBid = Bid.find("tender.id = ?1 order by amount asc", id).firstResult();

        if (winningBid == null) {
            return Response.ok()
                    .entity("{\"message\": \"Gara chiusa. Nessuna offerta pervenuta.\", \"tenderId\": " + id + "}")
                    .build();
        }

        return Response.ok(new TenderAwardResult(tender, winningBid)).build();
    }

    public record TenderAwardResult(Tender tender, Bid winningBid) {}
}
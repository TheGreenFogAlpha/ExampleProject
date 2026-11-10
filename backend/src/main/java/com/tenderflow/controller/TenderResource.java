package com.tenderflow.controller;

import com.tenderflow.model.Bid;
import com.tenderflow.model.Tender;
import io.quarkus.panache.common.Parameters;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.math.BigDecimal;
import java.util.List;

@Path("/api/tenders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Tender Management", description = "Operazioni di gestione, ricerca e aggiudicazione delle gare d'appalto")
public class TenderResource {

    @Channel("tender-events-out")
    Emitter<Tender> tenderEmitter;

    @GET
    @Operation(summary = "Lista gare con filtri", description = "Recupera la lista delle gare con possibilità di filtrare per stato, range di budget e titolo.")
    @APIResponse(responseCode = "200", description = "Lista restituita con successo",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Tender.class)))
    public List<Tender> getTenders(
            @Parameter(description = "Stato della gara (es. OPEN, CLOSED)") @QueryParam("status") String status,
            @Parameter(description = "Budget minimo") @QueryParam("minBudget") BigDecimal minBudget,
            @Parameter(description = "Budget massimo") @QueryParam("maxBudget") BigDecimal maxBudget,
            @Parameter(description = "Parola chiave nel titolo") @QueryParam("title") String title) {

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
    @Transactional
    @Operation(summary = "Crea una nuova gara", description = "Persiste una nuova gara ed emette un evento Kafka sul topic tender-events.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Gara creata con successo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Tender.class))),
            @APIResponse(responseCode = "400", description = "Payload della richiesta non valido")
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
    @Transactional
    @Operation(summary = "Chiudi gara e calcola vincitore", description = "Imposta lo stato a CLOSED, pubblica l'evento su Kafka e identifica l'offerta piu' bassa.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Gara chiusa ed eventualmente aggiudicata",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TenderAwardResult.class))),
            @APIResponse(responseCode = "404", description = "Gara non trovata"),
            @APIResponse(responseCode = "400", description = "Gara gia' chiusa")
    })
    public Response closeTender(@Parameter(description = "ID della gara da chiudere") @PathParam("id") Long id) {
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
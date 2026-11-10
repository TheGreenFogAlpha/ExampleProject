package com.tenderflow.controller;

import com.tenderflow.model.Bid;
import com.tenderflow.model.Tender;
import io.quarkus.panache.common.Parameters;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.math.BigDecimal;
import java.util.List;

@Path("/api/tenders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TenderResource {

    @Channel("tender-events-out")
    Emitter<Tender> tenderEmitter;

    @GET
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
    @Transactional
    public Response create(Tender tender) {
        if (tender.id != null) {
            tender.id = null;
        }
        tender.persist();

        //Pubblica l'evento su kafka
        tenderEmitter.send(tender);

        return Response.status(Response.Status.CREATED).entity(tender).build();
    }

    @PUT
    @Path("/{id}/close")
    @Transactional
    public Response closeTender(@PathParam("id") Long id) {
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

        //Chiusura gara
        tender.status = "CLOSED";

        // Notifica cambio stato via Kafka
        tenderEmitter.send(tender);

        // calcolo offerta vincente (importo piu' basso)
        Bid winningBid = Bid.find("tender.id = ?1 order by amount asc", id).firstResult();

        if (winningBid == null) {
            return Response.ok()
                    .entity("{\"message\": \"Gara chiusa. Nessuna offerta pervenuta.\", \"tenderId\": " + id + "}")
                    .build();
        }

        return Response.ok(new TenderAwardResult(tender, winningBid)).build();
    }

    // DTO per il risultato dell'aggiudicazione
    public record TenderAwardResult(Tender tender, Bid winningBid) {}
}

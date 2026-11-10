package com.tenderflow.controller;

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
}

package com.tenderflow.controller;

import com.tenderflow.model.Bid;
import com.tenderflow.model.Tender;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;

@Path("/api/tenders/{tenderId}/bids")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BidResource {

    @POST
    @Transactional
    public Response createBid(@PathParam("tenderId") Long tenderId, Bid bid) {
        Tender tender = Tender.findById(tenderId);
        if (tender == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Gara non trovata\"}")
                    .build();
        }

        if (!"OPEN".equalsIgnoreCase(tender.status)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Impossibile inviare offerte per una gara non aperta\"}")
                    .build();
        }

        bid.tender = tender;
        bid.submissionDate = LocalDateTime.now();
        bid.persist();

        return Response.status(Response.Status.CREATED).entity(bid).build();
    }

    @GET
    public List<Bid> getBidsForTender(@PathParam("tenderId") Long tenderId) {
        return Bid.list("tender.id", tenderId);
    }
}
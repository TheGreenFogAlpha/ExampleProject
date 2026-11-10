package com.tenderflow.controller;

import com.tenderflow.model.Bid;
import com.tenderflow.model.Tender;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Path("/api/tenders/{tenderId}/bids")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bid Management", description = "Sottomissione e consultazione offerte per gara")
public class BidResource {

    @Channel("bid-events-out")
    Emitter<Bid> bidEmitter;

    @POST
    @RolesAllowed("SUPPLIER")
    @Transactional
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Invia offerta (Solo SUPPLIER)", description = "Richiede il ruolo SUPPLIER per sottomessa un'offerta.")
    public Response createBid(
            @Parameter(description = "ID della gara") @PathParam("tenderId") Long tenderId,
            Bid bid) {

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

        if (tender.closingDate != null && LocalDate.now().isAfter(tender.closingDate)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Il termine per la presentazione delle offerte per questa gara e' scaduto\"}")
                    .build();
        }

        if (bid.amount == null || bid.amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"L'importo dell'offerta deve essere maggiore di zero\"}")
                    .build();
        }

        if (tender.budget != null && bid.amount.compareTo(tender.budget) > 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"L'importo dell'offerta supera il budget massimo della gara\"}")
                    .build();
        }

        bid.tender = tender;
        bid.submissionDate = LocalDateTime.now();
        bid.persist();

        bidEmitter.send(bid);

        return Response.status(Response.Status.CREATED).entity(bid).build();
    }

    @GET
    @RolesAllowed({"BUYER", "SUPPLIER"})
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Recupera offerte di una gara", description = "Accessibile da utenti autenticati come BUYER o SUPPLIER.")
    public List<Bid> getBidsForTender(@PathParam("tenderId") Long tenderId) {
        return Bid.list("tender.id", tenderId);
    }
}
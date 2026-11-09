package com.tenderflow.controller;

import com.tenderflow.model.Tender;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.List;

@Path("/api/tenders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TenderResource {

    @Inject
    @Channel("tender-events-out")
    Emitter<Tender> tenderEmitter;

    @GET
    public List<Tender> listAll() {
        return Tender.listAll();
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

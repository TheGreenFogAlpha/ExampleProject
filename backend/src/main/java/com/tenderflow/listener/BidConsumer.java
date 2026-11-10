package com.tenderflow.listener;

import com.tenderflow.model.Bid;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class BidConsumer {

    private static final Logger LOG = Logger.getLogger(BidConsumer.class);

    @Incoming("bid-events-in")
    public void consume(Bid bid) {
        LOG.infof(" [KAFKA EVENT] Nuova offerta ricevuta! Offerente: %s | Importo: %s € | Gara ID: %d",
                bid.bidderName, bid.amount, bid.tender.id);
    }
}
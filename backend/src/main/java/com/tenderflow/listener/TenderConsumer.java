package com.tenderflow.listener;

import com.tenderflow.model.Tender;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TenderConsumer {

    private static final Logger LOG = Logger.getLogger(TenderConsumer.class);

    @Incoming("tender-events-in")
    public void consume(Tender tender) {
        LOG.infof(" [Kafka Consumer] Ricevuto nuovo evento per la gara: '%s' con budget: %.2f",
                tender.title, tender.budget);
    }
}


package com.tenderflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
public class Bid extends PanacheEntity {

    public String bidderName;
    public BigDecimal amount;
    public LocalDateTime submissionDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tender_id", nullable = false)
    @JsonIgnoreProperties({"description"})
    public Tender tender;

    public Bid() {
    }
}
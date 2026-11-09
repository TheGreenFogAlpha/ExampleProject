package com.tenderflow.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tenders")
public class Tender extends PanacheEntity {

    public String title;
    public String description;
    public BigDecimal budget;
    public LocalDate closingDate;
    public String status; // es. "OPEN", "CLOSED"

    public Tender() {
    }
}



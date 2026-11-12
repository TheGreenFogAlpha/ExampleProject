package com.tenderflow.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String email;

    @Column(nullable = false)
    public String password; // Verrà salvata cifrata (BCrypt)

    @Column(nullable = false)
    public String fullName;

    @Column(nullable = false)
    public String role; // "BUYER" oppure "SUPPLIER"

    public static User findByEmail(String email) {
        return find("email", email).firstResult();
    }
}
package com.tenderflow;

import io.smallrye.jwt.build.Jwt;

import java.util.Set;

public class GenerateToken {

    public static String generateBuyerToken() {
        return Jwt.issuer("https://tenderflow.com/issuer")
                .upn("buyer@tenderflow.com")
                .groups(Set.of("BUYER"))
                .expiresIn(3600)
                .sign();
    }

    public static String generateSupplierToken() {
        return Jwt.issuer("https://tenderflow.com/issuer")
                .upn("supplier@tenderflow.com")
                .groups(Set.of("SUPPLIER"))
                .expiresIn(3600)
                .sign();
    }

    public static void main(String[] args) {
        System.out.println("--- BUYER TOKEN ---");
        System.out.println(generateBuyerToken());
        System.out.println("\n--- SUPPLIER TOKEN ---");
        System.out.println(generateSupplierToken());
    }
}
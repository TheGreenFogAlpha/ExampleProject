package com.tenderflow;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class TenderResourceTest {

    @Test
    public void testTenderLifecycleAndBidding() {
        // 1. Creazione Gara
        String tenderJson = """
            {
                "title": "Gara Test Automatizzato",
                "description": "Fornitura IT",
                "budget": 50000.00,
                "closingDate": "2026-12-31",
                "status": "OPEN"
            }
            """;

        Integer tenderId = given()
                .contentType(ContentType.JSON)
                .body(tenderJson)
                .when()
                .post("/api/tenders")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .path("id");

        // 2. Invio Offerta Valida
        String bidJson = """
            {
                "bidderName": "Test Supplier Ltd",
                "amount": 35000.00
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(bidJson)
                .when()
                .post("/api/tenders/" + tenderId + "/bids")
                .then()
                .statusCode(201)
                .body("bidderName", is("Test Supplier Ltd"));

        // 3. Invio Offerta Fuori Budget (Fallimento atteso)
        String invalidBidJson = """
            {
                "bidderName": "Expensive Supplier",
                "amount": 90000.00
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidBidJson)
                .when()
                .post("/api/tenders/" + tenderId + "/bids")
                .then()
                .statusCode(400);

        // 4. Chiusura Gara e Aggiudicazione
        given()
                .when()
                .put("/api/tenders/" + tenderId + "/close")
                .then()
                .statusCode(200)
                .body("winningBid.bidderName", is("Test Supplier Ltd"));
    }
}
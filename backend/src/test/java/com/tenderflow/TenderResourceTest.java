package com.tenderflow;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.jwt.build.Jwt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class TenderResourceTest {

    private static String buyerToken;
    private static String supplierToken;

    @BeforeAll
    public static void setupTokens() {
        buyerToken = Jwt.issuer("https://tenderflow.com/issuer")
                .upn("buyer@tenderflow.com")
                .groups(Set.of("BUYER"))
                .expiresIn(3600)
                .sign();

        supplierToken = Jwt.issuer("https://tenderflow.com/issuer")
                .upn("supplier@tenderflow.com")
                .groups(Set.of("SUPPLIER"))
                .expiresIn(3600)
                .sign();
    }

    @Test
    public void testUnauthenticatedAccessFails() {
        String tenderJson = """
            {
                "title": "Gara Non Autorizzata",
                "budget": 10000.00,
                "status": "OPEN"
            }
            """;

        // Tentativo senza header Authorization -> 401 Unauthorized
        given()
                .contentType(ContentType.JSON)
                .body(tenderJson)
                .when()
                .post("/api/tenders")
                .then()
                .statusCode(401);
    }

    @Test
    public void testRoleAccessAndCompleteLifecycle() {
        String tenderJson = """
            {
                "title": "Gara Cyber Security",
                "description": "Fornitura SOC Managed",
                "budget": 60000.00,
                "closingDate": "2026-12-31",
                "status": "OPEN"
            }
            """;

        // 1. SUPPLIER tenta di creare una gara -> 403 Forbidden
        given()
                .auth().oauth2(supplierToken)
                .contentType(ContentType.JSON)
                .body(tenderJson)
                .when()
                .post("/api/tenders")
                .then()
                .statusCode(403);

        // 2. BUYER crea la gara -> 201 Created
        Integer tenderId = given()
                .auth().oauth2(buyerToken)
                .contentType(ContentType.JSON)
                .body(tenderJson)
                .when()
                .post("/api/tenders")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .path("id");

        // 3. BUYER tenta di inviare un'offerta -> 403 Forbidden
        String bidJson = """
            {
                "bidderName": "Buyer Corp",
                "amount": 40000.00
            }
            """;

        given()
                .auth().oauth2(buyerToken)
                .contentType(ContentType.JSON)
                .body(bidJson)
                .when()
                .post("/api/tenders/" + tenderId + "/bids")
                .then()
                .statusCode(403);

        // 4. SUPPLIER invia un'offerta valida -> 201 Created
        String validBidJson = """
            {
                "bidderName": "SecureTech SpA",
                "amount": 45000.00
            }
            """;

        given()
                .auth().oauth2(supplierToken)
                .contentType(ContentType.JSON)
                .body(validBidJson)
                .when()
                .post("/api/tenders/" + tenderId + "/bids")
                .then()
                .statusCode(201)
                .body("bidderName", is("SecureTech SpA"));

        // 5. SUPPLIER tenta di chiudere la gara -> 403 Forbidden
        given()
                .auth().oauth2(supplierToken)
                .when()
                .put("/api/tenders/" + tenderId + "/close")
                .then()
                .statusCode(403);

        // 6. BUYER chiude la gara -> 200 OK con aggiudicazione
        given()
                .auth().oauth2(buyerToken)
                .when()
                .put("/api/tenders/" + tenderId + "/close")
                .then()
                .statusCode(200)
                .body("winningBid.bidderName", is("SecureTech SpA"));
    }
}
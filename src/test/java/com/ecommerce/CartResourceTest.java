package com.ecommerce;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class CartResourceTest {

    @Test
    void testCartFlow() {
        // 1. Inicializar productos
        given().when().get("/api/cart/init").then().statusCode(200);

        // 2. Crear Carrito para "Juan"
        var cartId = given()
            .when().post("/api/cart/client/Juan")
            .then()
            .statusCode(200)
            .extract().path("id");

        // 3. Ver que se creó
        given()
            .when().get("/api/cart")
            .then()
            .statusCode(200)
            .body(containsString("Juan"));
    }
}
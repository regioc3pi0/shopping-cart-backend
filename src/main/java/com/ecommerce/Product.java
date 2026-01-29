package com.ecommerce;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Product extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;
    public BigDecimal price;
    public int stock;

    public static void add(String name, double price, int stock) {
        Product p = new Product();
        p.name = name;
        p.price = BigDecimal.valueOf(price);
        p.stock = stock;
        p.persist();
    }
}
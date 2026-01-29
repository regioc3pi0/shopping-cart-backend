package com.ecommerce;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/cart")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CartResource {

    @GET
    @Path("/init")
    @Transactional
    public List<Product> init() {
        CartItem.deleteAll();
        Cart.deleteAll();
        Product.deleteAll();

        try {
            Product.getEntityManager().createNativeQuery("DELETE FROM sqlite_sequence").executeUpdate();
        } catch (Exception e) {
 
        }

        Product.add("Laptop", 1200.00, 10);
        Product.add("Mouse", 25.00, 50);
        Product.add("Monitor", 300.00, 20);
        return Product.listAll();
    }

    @GET
    @Path("/products")
    public List<Product> getAllProducts() {
        return Product.listAll();
    }

    @POST
    @Path("/product")
    @Transactional
    public Response createProduct(Product product) {
        product.persist();
        return Response.status(201).entity(product).build();
    }

    @GET
    public List<Cart> getAll() {
        return Cart.listAll();
    }

    @POST
    @Path("/client/{name}")
    @Transactional
    public Cart createCart(@PathParam("name") String clientName) {
        Cart cart = new Cart();
        cart.clientName = clientName;
        cart.status = "OPEN";
        cart.persist();
        return cart;
    }

    @POST
    @Path("/{cartId}/product/{productId}/{qty}")
    @Transactional
    public Response addItem(@PathParam("cartId") Long cartId, 
                            @PathParam("productId") Long productId, 
                            @PathParam("qty") int qty) {
        
        Cart cart = Cart.findById(cartId);
        Product product = Product.findById(productId);

        if (cart == null || product == null) {
            return Response.status(404).entity("Carrito o Producto no encontrado").build();
        }

        if ("PROCESSED".equals(cart.status)) {
            return Response.status(400).entity("El carrito ya fue procesado").build();
        }


        CartItem item = null;
        for (CartItem i : cart.items) {
            if (i.product.id.equals(productId)) {
                item = i;
                break;
            }
        }

        if (item == null) {
            item = new CartItem();
            item.product = product;
            item.quantity = qty;
            item.persist();
            cart.items.add(item);
        } else {

            item.quantity += qty;
        }
        cart.persist(); 
        return Response.ok(cart).build();
    }

    @PUT
    @Path("/item/{itemId}/{qty}")
    @Transactional
    public Response updateQty(@PathParam("itemId") Long itemId, @PathParam("qty") int qty) {
        CartItem item = CartItem.findById(itemId);
        if (item == null) return Response.status(404).build();
        item.quantity = qty;
        return Response.ok(item).build();
    }

    @DELETE
    @Path("/{cartId}/item/{itemId}")
    @Transactional
    public Response removeItem(@PathParam("cartId") Long cartId, @PathParam("itemId") Long itemId) {
        Cart cart = Cart.findById(cartId);
        CartItem item = CartItem.findById(itemId);
        
        if (cart != null && item != null) {
            cart.items.remove(item);
            item.delete(); 
            return Response.ok(cart).build();
        }
        return Response.status(404).build();
    }

    @POST
    @Path("/{cartId}/checkout")
    @Transactional
    public Response checkout(@PathParam("cartId") Long cartId) {
        Cart cart = Cart.findById(cartId);
        if (cart == null) return Response.status(404).build();

        for (CartItem item : cart.items) {
            if (item.product.stock < item.quantity) {
                return Response.status(400)
                        .entity("Sin stock suficiente para: " + item.product.name).build();
            }
            item.product.stock -= item.quantity; 
        }

        cart.status = "PROCESSED";
        return Response.ok(cart).build();
    }
}


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

    // --- 0. INIT: Inicializar datos (CON ORDEN DE BORRADO CORRECTO) ---
    @GET
    @Path("/init")
    @Transactional
    public List<Product> init() {
        // 1. Borrar primero los hijos (Items) para no romper FK
        CartItem.deleteAll();
        // 2. Borrar Carritos
        Cart.deleteAll();
        // 3. Borrar Productos al final
        Product.deleteAll();

        // 4. Resetear contador de IDs de SQLite (Evita que el ID crezca infinitamente)
        try {
            Product.getEntityManager().createNativeQuery("DELETE FROM sqlite_sequence").executeUpdate();
        } catch (Exception e) {
            // Ignoramos si la tabla de secuencia no existe aun
        }

        // 5. Crear productos base
        Product.add("Laptop", 1200.00, 10);
        Product.add("Mouse", 25.00, 50);
        Product.add("Monitor", 300.00, 20);

        // Retornamos la lista para confirmar IDs
        return Product.listAll();
    }

    // --- 1. NUEVO: Listado de Productos ---
    @GET
    @Path("/products")
    public List<Product> getAllProducts() {
        return Product.listAll();
    }

    // --- 2. NUEVO: Dar de alta un producto (Solicitado) ---
    @POST
    @Path("/product")
    @Transactional
    public Response createProduct(Product product) {
        // Guardamos el producto que viene en el JSON
        product.persist();
        return Response.status(201).entity(product).build();
    }

    // --- 3. Listado de Carritos ---
    @GET
    public List<Cart> getAll() {
        return Cart.listAll();
    }

    // --- 4. Registrar Cliente (Crea carrito vacio) ---
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

    // --- 5. Agregar Producto al Carrito ---
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

        // Buscar si el item ya existe en el carrito
        CartItem item = null;
        for (CartItem i : cart.items) {
            if (i.product.id.equals(productId)) {
                item = i;
                break;
            }
        }

        if (item == null) {
            // Si no existe, creamos uno nuevo
            item = new CartItem();
            item.product = product;
            item.quantity = qty;
            item.persist();
            cart.items.add(item);
        } else {
            // Si existe, sumamos cantidad
            item.quantity += qty;
        }

        cart.persist(); 
        return Response.ok(cart).build();
    }

    // --- 6. Modificar cantidades ---
    @PUT
    @Path("/item/{itemId}/{qty}")
    @Transactional
    public Response updateQty(@PathParam("itemId") Long itemId, @PathParam("qty") int qty) {
        CartItem item = CartItem.findById(itemId);
        if (item == null) return Response.status(404).build();
        
        item.quantity = qty;
        return Response.ok(item).build();
    }

    // --- 7. Eliminar producto del carrito ---
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

    // --- 8. Procesar Pedido (Checkout) ---
    @POST
    @Path("/{cartId}/checkout")
    @Transactional
    public Response checkout(@PathParam("cartId") Long cartId) {
        Cart cart = Cart.findById(cartId);
        if (cart == null) return Response.status(404).build();

        // Validar stock y descontar
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

/*package com.ecommerce;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/cart")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CartResource {

    // Inicializar datos dummy para la prueba
    @GET
    @Path("/init")
    @Transactional
    public String init() {
        Product.deleteAll();
        Product.add("Laptop", 1200.00, 10);
        Product.add("Mouse", 25.00, 50);
        Product.add("Monitor", 300.00, 20);
        return "Datos inicializados";
    }

    // 1. Registrar Cliente (Creamos un carrito vacío para él)
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

    // Listar carritos
    @GET
    public List<Cart> getAll() {
        return Cart.listAll();
    }

    // 2. Agregar Producto al Carrito
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

        if (cart.status.equals("PROCESSED")) {
            return Response.status(400).entity("El carrito ya fue procesado").build();
        }

        CartItem item = new CartItem();
        item.product = product;
        item.quantity = qty;
        item.persist(); // Guardamos el item antes de asociarlo

        cart.items.add(item);
        cart.persist(); // Actualizamos carrito

        return Response.ok(cart).build();
    }

    // 3. Modificar cantidades
    @PUT
    @Path("/item/{itemId}/{qty}")
    @Transactional
    public Response updateQty(@PathParam("itemId") Long itemId, @PathParam("qty") int qty) {
        CartItem item = CartItem.findById(itemId);
        if (item == null) return Response.status(404).build();
        
        item.quantity = qty;
        return Response.ok(item).build();
    }

    // 4. Eliminar producto del carrito
    @DELETE
    @Path("/{cartId}/item/{itemId}")
    @Transactional
    public Response removeItem(@PathParam("cartId") Long cartId, @PathParam("itemId") Long itemId) {
        Cart cart = Cart.findById(cartId);
        CartItem item = CartItem.findById(itemId);
        
        if (cart != null && item != null) {
            cart.items.remove(item);
            item.delete(); // Borrado físico
            return Response.ok(cart).build();
        }
        return Response.status(404).build();
    }

    // 5. Procesar Pedido
    @POST
    @Path("/{cartId}/checkout")
    @Transactional
    public Response checkout(@PathParam("cartId") Long cartId) {
        Cart cart = Cart.findById(cartId);
        if (cart == null) return Response.status(404).build();

        // Lógica simple: Descontar stock y cerrar carrito
        for (CartItem item : cart.items) {
            if (item.product.stock < item.quantity) {
                return Response.status(400)
                        .entity("Sin stock suficiente para: " + item.product.name).build();
            }
            item.product.stock -= item.quantity; // Hibernate actualiza esto solo
        }

        cart.status = "PROCESSED";
        return Response.ok(cart).build();
    }
}*/
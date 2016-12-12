package com.patientping.hiring.web;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patientping.hiring.core.*;
import com.patientping.hiring.persistence.OrderRepository;
import com.patientping.hiring.persistence.RecordLimiter;
import com.patientping.hiring.serialization.EntityWrapper;
import com.patientping.hiring.serialization.JsonApiDocument;
import com.patientping.hiring.serialization.Relationship;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import org.skife.jdbi.v2.TransactionStatus;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

@Path("/{o:orders?}")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {
  static final TypeReference<Relationship<EntityWrapper<User>>> USER_RELATION_TYPE_REFERENCE = new TypeReference<Relationship<EntityWrapper<User>>>(){};
  static final TypeReference<Relationship<EntityWrapper<Show>>> SHOW_RELATION_TYPE_REFERENCE = new TypeReference<Relationship<EntityWrapper<Show>>>(){};

  private final RecordLimiter recordLimiter;
  private final OrderRepository orderRepository;
  private final ObjectMapper objectMapper;

  public OrderResource(RecordLimiter recordLimiter, OrderRepository orderRepository, ObjectMapper objectMapper) {
    this.recordLimiter = recordLimiter;
    this.orderRepository = orderRepository;
    this.objectMapper = objectMapper;
  }

  @GET
  @Timed
  public Response findOrders(
      @QueryParam("filter") final String filter,
      @QueryParam("order") final String order,
      @QueryParam("skip") final int skip,
      @QueryParam("limit") final int limit,
      @Auth Token token) throws IOException {
    try {
      List<Order> result = orderRepository.findOrders(filter, Ordering.parse(order), skip, recordLimiter.limit(limit), token);
      return Response.ok()
          .entity(new JsonApiDocument<>(result))
          .header("X-Total-Count", orderRepository.count(filter, token)) // Count in header, probably should go in meta, but convention not yet solid as to how.
          .build();
    } catch (final IllegalArgumentException ex) {
      throw new ClientErrorException(ex.getMessage(), Response.Status.BAD_REQUEST);
    }
  }

  @GET
  @Path("{id}")
  @Timed
  public JsonApiDocument<Order> getOrder(@PathParam("id") final long id, @Auth Token token) throws IOException {
    return new JsonApiDocument<>(orderRepository.getOrder(id));
  }

  @POST
  @Timed
  public Response createOrder(final JsonApiDocument<EntityWrapper<Order>> document,
                                  @Auth final Token token) throws IOException{
    final EntityWrapper<Order> entity = document.getData();
    final Object buyerObject = entity.getLinks().get("buyer");
    final Object showObject = entity.getLinks().get("show");
    final Order order = new Order(null,
        new User(buyerObject != null ? ((Number)objectMapper.<Relationship<EntityWrapper<User>>>convertValue(buyerObject, USER_RELATION_TYPE_REFERENCE).getLinkage().getId()).longValue() : token.getUserId(), null),
        new Show(((Number)objectMapper.<Relationship<EntityWrapper<Show>>>convertValue(showObject, SHOW_RELATION_TYPE_REFERENCE).getLinkage().getId()).longValue(), null),
        entity.getAttributes().getTickets());

    if(!token.isAdmin() && order.getBuyer() != null && order.getBuyer().getId() != token.getUserId()) {
      throw new ForbiddenException("Cannot order tickets for a different user");
    }
    if(order.getTickets() == null || order.getTickets() <= 0) {
      throw new WebApplicationException("Number of tickets is required. Must be a positive integer", 422);
    }
    if(order.getShow() == null || order.getShow().getId() == null) {
      throw new WebApplicationException("Show is required");
    }
    final long id = orderRepository.createOrder(order, token);
    return Response.created(URI.create("/orders/" + Long.toString(id))).build(); // consider returning new order in body
  }

  @PATCH
  @Timed
  public Response patchOrders(final JsonApiDocument<Collection<EntityWrapper<Order>>> document,
                              @Auth final Token token) throws IOException {
    orderRepository.inTransaction((OrderRepository orderRepository, TransactionStatus status) -> {
      for(EntityWrapper<Order> entity: document.getData()) {
        final Object buyerObject = entity.getLinks() != null ? entity.getLinks().get("buyer") : null;
        final Object showObject = entity.getLinks() != null ? entity.getLinks().get("show") : null;
        final Order order = new Order(entity.getId() != null ? ((Number)entity.getId()).longValue() : null,
            buyerObject != null ? new User(((Number)objectMapper.<Relationship<EntityWrapper<User>>>convertValue(buyerObject, USER_RELATION_TYPE_REFERENCE).getLinkage().getId()).longValue(), null) : null ,
            showObject != null ? new Show(((Number)objectMapper.<Relationship<EntityWrapper<Show>>>convertValue(showObject, SHOW_RELATION_TYPE_REFERENCE).getLinkage().getId()).longValue(), null) : null,
            entity.getAttributes() != null ? entity.getAttributes().getTickets() : null);
        if(order.getId() == null) {
          orderRepository.createOrder(order, token);
        } else if (entity.getAttributes().getTickets() == 0) {
          orderRepository.deleteOrder(order.getId());
        } else {
          orderRepository.updateOrder(order, token);
        }
      }
      return null;
    });

    return Response.noContent().build(); // return empty 202, consider a collection of newly created and patched (and deleted?) orders
  }

  @PATCH
  @Path("{id}")
  @Timed
  public Response updateOrder(@PathParam("id") final long id,
                        final JsonApiDocument<EntityWrapper<Order>> document,
                        @Auth Token token) throws IOException {
    final EntityWrapper<Order> entity = document.getData();
    if(entity.getId() != null && ((Number)entity.getId()).longValue() != id) {
      throw new WebApplicationException("Order ids do not match. Cannot update order id.", 422);
    }
    final Object buyerObject = entity.getLinks() != null ? entity.getLinks().get("buyer") : null;
    final Object showObject = entity.getLinks() != null ? entity.getLinks().get("show") : null;
    final Order order = new Order(id,
        buyerObject != null ? new User(((Number)objectMapper.<Relationship<EntityWrapper<User>>>convertValue(buyerObject, USER_RELATION_TYPE_REFERENCE).getLinkage().getId()).longValue(), null) : null ,
        showObject != null ? new Show(((Number)objectMapper.<Relationship<EntityWrapper<Show>>>convertValue(showObject, SHOW_RELATION_TYPE_REFERENCE).getLinkage().getId()).longValue(), null) : null,
        entity.getAttributes() != null ? entity.getAttributes().getTickets() : null);

    orderRepository.updateOrder(order, token);
    return Response.noContent().build();
  }

  @DELETE
  @Path("{id}")
  @Timed
  public Response deleteOrder(@PathParam("id") final long id, @Auth Token token) throws IOException {
    orderRepository.deleteOrder(id, token);
    return Response.noContent().build(); // Always 202 to prevent data leakage
  }
}

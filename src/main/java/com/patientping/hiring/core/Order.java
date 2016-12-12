package com.patientping.hiring.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.patientping.hiring.serialization.Entity;
import com.patientping.hiring.serialization.Link;
import com.patientping.hiring.serialization.Self;
import com.patientping.hiring.serialization.Type;

@Entity
@Self("orders/<id>")
@Type("order")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Order {
  private final Long id;
  private final User buyer;
  private final Show show;
  private final Integer tickets;

  @JsonCreator
  public Order(@JsonProperty("id") Long id, @JsonProperty("buyer") User buyer, @JsonProperty("show") Show show, @JsonProperty("tickets") Integer tickets) {
    this.id = id;
    this.buyer = buyer;
    this.show = show;
    this.tickets = tickets;
  }

  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  @Link
  @Self("order/<id>/buyer")
  @JsonProperty("buyer")
  public User getBuyer() {
    return buyer;
  }

  @Link
  @Self("order/<id>/show")
  @JsonProperty("show")
  public Show getShow() {
    return show;
  }

  @JsonProperty("tickets")
  public Integer getTickets() {
    return tickets;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Order order = (Order) o;

    return id != null && order.id != null && id.equals(order.id);
  }

  @Override
  public int hashCode() {
    return id == null ? super.hashCode() : id.hashCode();
  }
}

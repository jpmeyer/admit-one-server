package com.patientping.hiring.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.patientping.hiring.serialization.Entity;
import com.patientping.hiring.serialization.Self;
import com.patientping.hiring.serialization.Type;

import java.time.Instant;

@Entity
@Self("shows/<id>")
@Type("show")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Show {
  private final Long id;
  private final Instant doorsOpen;

  @JsonCreator
  public Show(@JsonProperty("id") Long id, @JsonProperty("doorsOpen") Instant doorsOpen) {
    this.id = id;
    this.doorsOpen = doorsOpen;
  }

  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  @JsonProperty("doorsOpen")
  public Instant getDoorsOpen() {
    return doorsOpen;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Show show = (Show) o;

    return id != null && show.id != null && id.equals(show.id);
  }

  @Override
  public int hashCode() {
    return id == null ? super.hashCode() : id.hashCode();
  }
}

package com.patientping.hiring.serialization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public class Relationship<T> {
  static final String SELF_KEY = "self";
  static final String LINKAGE_KEY = "linkage";

  private final URI self;
  private final T linkage;

  @JsonCreator
  public Relationship(@JsonProperty(SELF_KEY) final URI self, @JsonProperty(LINKAGE_KEY) final T linkage) {
    this.self = self;
    this.linkage = linkage;
  }

  @JsonProperty(SELF_KEY)
  public URI getSelf() {
    return self;
  }

  @JsonProperty(LINKAGE_KEY)
  public T getLinkage() {
    return linkage;
  }
}

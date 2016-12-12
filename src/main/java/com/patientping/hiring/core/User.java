package com.patientping.hiring.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.patientping.hiring.serialization.Entity;
import com.patientping.hiring.serialization.Self;
import com.patientping.hiring.serialization.Type;

@Entity
@Self("users/<id>")
@Type("user")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
  private final Long id;
  private final String login;

  public User(@JsonProperty("id") Long id, @JsonProperty("login") String login) {
    this.id = id;
    this.login = login;
  }

  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  @JsonProperty("login")
  public String getLogin() {
    return login;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    User user = (User) o;

    return id != null && user.id != null && id.equals(user.id);
  }

  @Override
  public int hashCode() {
    return id == null ? super.hashCode() : id.hashCode();
  }
}

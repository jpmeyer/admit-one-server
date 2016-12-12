package com.patientping.hiring.core;


import java.security.Principal;

public class Token implements Principal {
  private final long userId;
  private final boolean admin;

  public Token(long userId, boolean admin) {
    this.userId = userId;
    this.admin = admin;
  }

  public long getUserId() {
    return userId;
  }

  public boolean isAdmin() {
    return admin;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Token token = (Token) o;

    return userId == token.userId;
  }

  @Override
  public String toString() {
    return Long.toString(userId);
  }

  @Override
  public int hashCode() {
    return Long.hashCode(userId);
  }

  @Override
  public String getName() {
    return Long.toString(userId);
  }
}

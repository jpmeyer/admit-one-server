package com.patientping.hiring.web;

import com.patientping.hiring.core.Token;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

import java.util.Optional;

public class OAuthAuthenticator implements Authenticator<String, Token> {
  @Override
  public Optional<Token> authenticate(String credentials) throws AuthenticationException {
    try {
      return Optional.of(new Token(Long.parseLong(credentials), false));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}

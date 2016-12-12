package com.patientping.hiring.web;

import com.patientping.hiring.core.Token;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;

import java.util.Optional;

public class AdminBasicAuthenticator implements Authenticator<BasicCredentials, Token> {
  private final String password;

  public AdminBasicAuthenticator(String password) {
    this.password = password;
  }

  @Override
  public Optional<Token> authenticate(BasicCredentials credentials) throws AuthenticationException {
    return "root".equals(credentials.getUsername()) && password.equals(credentials.getPassword()) ?
        Optional.of(new Token(0, true)) :
        Optional.empty();
  }
}

package com.patientping.hiring.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.patientping.hiring.AdmitOneApplication;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

public final class Fixtures {
  public static final ObjectMapper MAPPER = AdmitOneApplication.createDefaultMapper();

  public static final User USER = new User(
      9000L,
      "john.smith"
  );

  public static final Show SHOW = new Show(
      9001L,
      Instant.parse("2020-01-01T12:34:56Z")
  );

  public static final Order ORDER = new Order(
      9090L,
      USER,
      SHOW,
      20
  );

  private Fixtures() {
  }
}
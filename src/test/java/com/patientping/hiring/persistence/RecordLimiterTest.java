package com.patientping.hiring.persistence;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RecordLimiterTest {
  @Test
  public void defaultLimit() {
    assertThat(new RecordLimiter(20, 100).limit(0), is(20));
  }

  @Test
  public void maximumLimit() {
    assertThat(new RecordLimiter(20, 100).limit(200), is(100));
  }

  @Test
  public void validLimit() {
    assertThat(new RecordLimiter(20, 100).limit(50), is(50));
  }

  @Test
  public void invalidLimit() {
    assertThat(new RecordLimiter(20, 100).limit(-1), is(20));
  }
}
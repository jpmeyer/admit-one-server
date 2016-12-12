package com.patientping.hiring.persistence;

public final class RecordLimiter {
  private final int defaultLimit;
  private final int maximumLimit;

  public RecordLimiter(final int defaultLimit, final int maximumLimit) {
    this.defaultLimit = defaultLimit;
    this.maximumLimit = maximumLimit;
  }

  public final int limit(final int limit) {
    if(limit <= 0)
      return defaultLimit;
    if(limit >= maximumLimit)
      return maximumLimit;
    return limit;
  }
}

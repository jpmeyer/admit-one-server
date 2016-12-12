package com.patientping.hiring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableCollection;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

public class AdmitOneConfiguration extends Configuration {
  @JsonProperty("database")
  private DataSourceFactory database = new DataSourceFactory();

  @Valid
  @NotNull
  @JsonProperty("database")
  public DataSourceFactory getDataSourceFactory() {
    return database;
  }

  @JsonProperty("default_limit")
  private int defaultLimit = 20;

  @Valid
  @JsonProperty("default_limit")
  public int getDefaultLimit() {
    return defaultLimit;
  }


  @JsonProperty("maximum_limit")
  private int maximumLimit = 1024;

  @Valid
  @JsonProperty("maximum_limit")
  public int getMaximumLimit() {
    return maximumLimit;
  }

  @JsonProperty("realm")
  private String realm = "AdmitOne";

  @JsonProperty("realm")
  public String getRealm() {
    return realm;
  }

  @JsonProperty("root_password")
  private String rootPassword = "correct horse battery staple";

  @JsonProperty("root_password")
  public String getRootPassword() {
    return rootPassword;
  }
}

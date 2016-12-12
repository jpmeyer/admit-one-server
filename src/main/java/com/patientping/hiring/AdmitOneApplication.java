package com.patientping.hiring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import com.patientping.hiring.core.Token;
import com.patientping.hiring.persistence.OrderMapper;
import com.patientping.hiring.persistence.OrderRepository;
import com.patientping.hiring.persistence.RecordLimiter;
import com.patientping.hiring.serialization.JsonApiModule;
import com.patientping.hiring.web.AdminBasicAuthenticator;
import com.patientping.hiring.web.OAuthAuthenticator;
import com.patientping.hiring.web.OrderResource;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.skife.jdbi.v2.DBI;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

public class AdmitOneApplication extends Application<AdmitOneConfiguration> {

  static final ObjectMapper OBJECT_MAPPER = createDefaultMapper();

  public static void main(final String[] args) throws Exception {
    new AdmitOneApplication().run(args);
  }

  @Override
  public String getName() {
    return "PatientPing Admit One Server";
  }

  @Override
  public void initialize(final Bootstrap<AdmitOneConfiguration> bootstrap) {
    bootstrap.addBundle(new MigrationsBundle<AdmitOneConfiguration>() {
      @Override
      public DataSourceFactory getDataSourceFactory(AdmitOneConfiguration configuration) {
        return configuration.getDataSourceFactory();
      }
    });
    bootstrap.setObjectMapper(OBJECT_MAPPER);
    super.initialize(bootstrap);
  }

  @Override
  public void run(final AdmitOneConfiguration configuration, final Environment environment) throws Exception {
    configureCors(environment);

    AuthFilter basicCredentialAuthFilter = new BasicCredentialAuthFilter.Builder<Token>()
        .setAuthenticator(new AdminBasicAuthenticator(configuration.getRootPassword()))
        .setPrefix("Basic")
        .setRealm(configuration.getRealm())
        .buildAuthFilter();

    AuthFilter oauthCredentialAuthFilter = new OAuthCredentialAuthFilter.Builder<Token>()
        .setAuthenticator(new OAuthAuthenticator())
        .setPrefix("Bearer")
        .buildAuthFilter();

    environment.jersey().register(new AuthDynamicFeature(new ChainedAuthFilter(ImmutableList.of(basicCredentialAuthFilter, oauthCredentialAuthFilter))));
    environment.jersey().register(new AuthValueFactoryProvider.Binder<>(Token.class));


    final DBI jdbi = new DBIFactory().build(environment, configuration.getDataSourceFactory(), "mysql");

    jdbi.registerMapper(new OrderMapper());

    final RecordLimiter recordLimiter = new RecordLimiter(configuration.getDefaultLimit(), configuration.getMaximumLimit());

    final OrderRepository orderRepository = jdbi.onDemand(OrderRepository.class);

    environment.jersey().register(new OrderResource(recordLimiter, orderRepository, OBJECT_MAPPER));
  }

  public static ObjectMapper createDefaultMapper() {
    return Jackson.newObjectMapper()
        .findAndRegisterModules()
        .registerModule(new JsonApiModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  static void configureCors(final Environment environment) {
    final FilterRegistration.Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    filter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS,PATCH");
    filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
    filter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
    filter.setInitParameter(CrossOriginFilter.EXPOSED_HEADERS_PARAM, "X-Total-Count");
    filter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin");
    filter.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");
    filter.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, "false");
  }
}

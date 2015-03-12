package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.security.AdaptAuthorizationFilter;
import at.ac.univie.isc.asio.security.FindAuthorization;
import at.ac.univie.isc.asio.security.NoTranslation;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.security.TranslateAuthorization;
import at.ac.univie.isc.asio.security.uri.NoopRule;
import at.ac.univie.isc.asio.security.uri.RuleBasedFinder;
import at.ac.univie.isc.asio.security.uri.StaticRedirect;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

import javax.servlet.DispatcherType;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
class DefaultSecurity {
  private static final Logger log = getLogger(DefaultSecurity.class);

  /**
   * use this order to override default settings
   */
  public static final int OVERRIDE_DEFAULT = Ordered.LOWEST_PRECEDENCE - 1000;

  public static final String REALM_NAME = "asio-nest";

  @Autowired
  private AsioSettings config;

  /**
   * Create a user for each existing role with the given password in the given user repository.
   */
  static AuthenticationManagerBuilder registerUsersForRoles(
      InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> auth,
      final String password) {
    for (Role role : Role.values()) {
      auth = auth.withUser(role.name()).password(password).authorities(role).and();
    }
    return auth.and();
  }

  @Bean
  @ConditionalOnProperty("spring.security.debug")
  public org.springframework.security.access.event.LoggerListener debugAuthorizationEventLogger() {
    return new org.springframework.security.access.event.LoggerListener();
  }

  @Bean
  @ConditionalOnProperty("spring.security.debug")
  public org.springframework.security.authentication.event.LoggerListener debugAuthenticationEventLogger() {
    return new org.springframework.security.authentication.event.LoggerListener();
  }

  @Autowired
  public void configureGlobal(final AuthenticationManagerBuilder auth) throws Exception {
    log.info(Scope.SYSTEM.marker(), "registering root user");
    auth.inMemoryAuthentication().withUser("root").password(config.secret).authorities(Role.ADMIN);
    if (config.feature.simpleAuth) {
      registerUsersForRoles(auth.inMemoryAuthentication(), config.secret);
    }
  }

  /**
   * Order the AdaptAuthorizationFilter before the spring security filter chain
   */
  public static final int AUTHORIZATION_ADAPTER_ORDER = SecurityProperties.DEFAULT_FILTER_ORDER - 5;

  @Bean
  public FilterRegistrationBean adaptAuthorizationFilter(final FindAuthorization finder,
                                                         final TranslateAuthorization translator) {
    final AdaptAuthorizationFilter filter = new AdaptAuthorizationFilter(finder, translator);
    final FilterRegistrationBean registration = new FilterRegistrationBean(filter);
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(DispatcherType.REQUEST);
    registration.setAsyncSupported(true);
    registration.setOrder(AUTHORIZATION_ADAPTER_ORDER);
    registration.setName("auth-rewrite-filter");
    return registration;
  }

  @Bean
  public StaticRedirect rewriteStaticContent() {
    return StaticRedirect.create("/explore/", "/static/");
  }

  @Bean
  public FindAuthorization defaultFinder(final StaticRedirect staticRedirect) {
    return RuleBasedFinder.create(StaticRedirect.URI_WITH_SCHEMA_HEAD,
        staticRedirect, NoopRule.instance());
  }

  @Bean
  public TranslateAuthorization defaultTranslation() {
    return NoTranslation.create();
  }

  @Bean
  public AuthenticationEntryPoint defaultAuthenticationEntryPoint() throws Exception {
    final BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
    entryPoint.setRealmName(REALM_NAME);
    entryPoint.afterPropertiesSet();
    return entryPoint;
  }
}

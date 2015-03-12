package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.security.FindAuthorization;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.security.TranslateAuthorization;
import at.ac.univie.isc.asio.security.TranslateToDelegateAuthorization;
import at.ac.univie.isc.asio.security.uri.ExtractRole;
import at.ac.univie.isc.asio.security.uri.RuleBasedFinder;
import at.ac.univie.isc.asio.security.uri.StaticRedirect;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Override default configurations to support URI based authorization for VPH deployments.
 */
@Configuration
@Order(DefaultSecurity.OVERRIDE_DEFAULT)
@ConditionalOnProperty(value = "asio.feature.vph-uri-auth", havingValue = "true")
class UriBasedSecurity {
  private static final Logger log = getLogger(UriBasedSecurity.class);

  private final String secret = UUID.randomUUID().toString();

  @PostConstruct
  public void report() {
    log.info(Scope.SYSTEM.marker(), "feature.vph-uri-auth active");
  }

  /**
   * Add each existing {@link at.ac.univie.isc.asio.security.Role role name} as valid login,
   * with the authorities granted to the role, to the global auth manager.
   */
  @Autowired
  public void configureGlobal(final AuthenticationManagerBuilder auth) throws Exception {
    log.info(Scope.SYSTEM.marker(), "registering {} as users with secret {}", Arrays.toString(Role.values()), secret);
    DefaultSecurity.registerUsersForRoles(auth.inMemoryAuthentication(), secret);
  }

  // override default authorization filter components

  @Bean
  @Primary
  public FindAuthorization extractRoleFinder(final StaticRedirect staticRedirect) {
    return RuleBasedFinder.create(ExtractRole.URI_WITH_ROLE_REGEX,
        staticRedirect, ExtractRole.instance());
  }

  @Bean
  @Primary
  public TranslateAuthorization defaultTranslation() {
    return TranslateToDelegateAuthorization.withSecret(secret);
  }

  /**
   * Challenging clients won't work with uri-based authorization
   */
  @Bean
  @Primary
  public AuthenticationEntryPoint forbiddenEntryPoint() {
    return new Http403ForbiddenEntryPoint();
  }
}

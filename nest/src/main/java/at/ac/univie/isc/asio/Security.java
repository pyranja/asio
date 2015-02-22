package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesUserDetailsService;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
class Security extends WebSecurityConfigurerAdapter {

  public static final String ASIO_PERMISSION_HEADER = "Permission";
  public static final String REALM_NAME = "asio-nest";

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Value("${spring.security.debug:false}")
  private boolean debug = false;
  @Autowired  // will be springs global authentication manager
  private AuthenticationManager springBootManager;

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

  @Override
  protected void configure(final HttpSecurity http) throws Exception {
    // @formatter:off
    http
      .authorizeRequests().anyRequest().fullyAuthenticated()
        .and()
      .addFilter(basicAuthenticationFilter())
      .addFilterAfter(preAuthenticationFilter(), BasicAuthenticationFilter.class)
        .authenticationProvider(preAuthenticationProvider())
      .exceptionHandling().authenticationEntryPoint(basicAuthenticationEntryPoint())
        .and()
      .anonymous().disable()
      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
      .csrf().disable()
      .logout().disable()
      .headers()
        .cacheControl()
        .contentTypeOptions()
        .xssProtection()
        .frameOptions()
    ;
    // @formatter:on
  }

  @Override
  protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
    auth
        .eraseCredentials(false)
        .parentAuthenticationManager(springBootManager)
    ;
  }

  @Override
  public void configure(final WebSecurity web) throws Exception {
    web.debug(debug);
  }

  private BasicAuthenticationFilter basicAuthenticationFilter() throws Exception {
    final BasicAuthenticationFilter filter = new BasicAuthenticationFilter(authenticationManagerBean());
    filter.setAuthenticationDetailsSource(preAuthenticatedDetailsSource());
    filter.afterPropertiesSet();
    return filter;
  }

  private BasicAuthCredentialDelegatingFilter preAuthenticationFilter() throws Exception {
    final BasicAuthCredentialDelegatingFilter filter =
        BasicAuthCredentialDelegatingFilter.create(authenticationManagerBean());
    filter.setAuthenticationDetailsSource(preAuthenticatedDetailsSource());
    filter.setApplicationEventPublisher(eventPublisher);
    filter.setContinueFilterChainOnUnsuccessfulAuthentication(true);
    filter.afterPropertiesSet();
    return filter;
  }

  private AuthenticationDetailsSource<HttpServletRequest, ?> preAuthenticatedDetailsSource() {
    return new HeaderAuthorizationDetailsSource(ASIO_PERMISSION_HEADER,
        RoleToPermissionMapper.instance(), GetMethodRestriction.exclude(Permission.INVOKE_UPDATE));
  }

  private AuthenticationEntryPoint basicAuthenticationEntryPoint() throws Exception {
    final BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
    entryPoint.setRealmName(REALM_NAME);
    entryPoint.afterPropertiesSet();
    return entryPoint;
  }

  private PreAuthenticatedAuthenticationProvider preAuthenticationProvider() {
    final PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
    provider.setThrowExceptionWhenTokenRejected(true);
    provider.setPreAuthenticatedUserDetailsService(new PreAuthenticatedGrantedAuthoritiesUserDetailsService());
    provider.afterPropertiesSet();
    return provider;
  }
}

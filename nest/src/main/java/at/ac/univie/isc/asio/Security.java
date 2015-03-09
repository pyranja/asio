package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.DispatcherType;
import java.util.Arrays;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
class Security extends WebSecurityConfigurerAdapter {

  public static final String REALM_NAME = "asio-nest";

  @SuppressWarnings("SpringJavaAutowiringInspection")
  @Autowired
  private AsioSettings config;

  @Value("${spring.security.debug:false}")
  private boolean debug = false;
  @Value("${asio.feature.vph-uri-auth:false}")
  private boolean uriAuth = false;

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

  /** Order the AdaptAuthorizationFilter before the spring security filter chain */
  public static final int AUTHORIZATION_ADAPTER_ORDER = SecurityProperties.DEFAULT_FILTER_ORDER - 5;

  @Bean(name = "uriAuthorizationFilter")
  @Primary
  @ConditionalOnProperty(value = "asio.feature.vph-uri-auth", havingValue = "true")
  public FilterRegistrationBean uriAuthorizationFilter() {
    final FindAuthorization authorizer = VphUriRewriter.withPrefixes("/catalog", "/explore");
    final TranslateAuthorization adapter = TranslateToDelegateAuthorization.withSecret(config.secret);
    final AdaptAuthorizationFilter filter = new AdaptAuthorizationFilter(authorizer, adapter);
    final FilterRegistrationBean registration = new FilterRegistrationBean(filter);
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(DispatcherType.REQUEST);
    registration.setAsyncSupported(true);
    registration.setOrder(AUTHORIZATION_ADAPTER_ORDER);
    registration.setName("uri-auth-filter");
    return registration;
  }

  @Bean(name = "uriAuthorizationFilter")
  public FilterRegistrationBean rewriteFilter() {
    final FindAuthorization authorizer = StaticCatalogRewriter.withPrefixes("/catalog", "/explore");
    final TranslateAuthorization adapter = NoTranslation.create();
    final AdaptAuthorizationFilter filter = new AdaptAuthorizationFilter(authorizer, adapter);
    final FilterRegistrationBean registration = new FilterRegistrationBean(filter);
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(DispatcherType.REQUEST);
    registration.setAsyncSupported(true);
    registration.setOrder(AUTHORIZATION_ADAPTER_ORDER);
    registration.setName("rewrite-filter");
    return registration;
  }

  @Override
  protected void configure(final HttpSecurity http) throws Exception {
    // @formatter:off
    http
      .authorizeRequests().anyRequest().fullyAuthenticated()
        .and()
      .addFilter(basicAuthenticationFilter())
      .exceptionHandling().authenticationEntryPoint(basicAuthenticationEntryPoint())
        .and()
      .anonymous().principal(Identity.undefined()).authorities(Arrays.<GrantedAuthority>asList(Role.NONE))
        .and()
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
    registerUsersForRoles(auth.inMemoryAuthentication())
        .eraseCredentials(false)
        .authenticationProvider(delegationAwareAuthenticationProvider())
    ;
  }

  private AuthenticationManagerBuilder registerUsersForRoles(InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> auth) {
    for (Role role : Role.values()) {
      auth = auth.withUser(role.name()).password(config.secret).authorities(role).and();
    }
    return auth.and();
  }

  @Override
  public void configure(final WebSecurity web) throws Exception {
    web.debug(debug);
  }

  private BasicAuthenticationFilter basicAuthenticationFilter() throws Exception {
    final BasicAuthenticationFilter filter = new BasicAuthenticationFilter(authenticationManagerBean());
    filter.setAuthenticationDetailsSource(DelegationDetailsSource.create());
    filter.afterPropertiesSet();
    return filter;
  }

  // FIXME : clean this up
  private AuthenticationEntryPoint basicAuthenticationEntryPoint() throws Exception {
    if (uriAuth) {
      return new Http403ForbiddenEntryPoint();
    } else {
      final BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
      entryPoint.setRealmName(REALM_NAME);
      entryPoint.afterPropertiesSet();
      return entryPoint;
    }
  }

  private DelegationAwareAuthenticationProvider delegationAwareAuthenticationProvider() throws Exception {
    final DelegationAwareAuthenticationProvider provider = new DelegationAwareAuthenticationProvider();
    provider.setAuthoritiesMapper(ExpandAuthoritiesContainer.instance());
    provider.setUserDetailsService(userDetailsServiceBean());
    provider.afterPropertiesSet();
    return provider;
  }

}

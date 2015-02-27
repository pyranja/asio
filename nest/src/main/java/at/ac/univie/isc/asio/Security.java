package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

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

  private AuthenticationEntryPoint basicAuthenticationEntryPoint() throws Exception {
    final BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
    entryPoint.setRealmName(REALM_NAME);
    entryPoint.afterPropertiesSet();
    return entryPoint;
  }

  private DelegationAwareAuthenticationProvider delegationAwareAuthenticationProvider() throws Exception {
    final DelegationAwareAuthenticationProvider provider = new DelegationAwareAuthenticationProvider();
    provider.setAuthoritiesMapper(ExpandAuthoritiesContainer.instance());
    provider.setUserDetailsService(userDetailsServiceBean());
    provider.afterPropertiesSet();
    return provider;
  }

}

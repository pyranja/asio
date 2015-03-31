package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.util.Collections;

/**
 * The internal authentication and authorization mechanism based on stateless basic authentication.
 * Support 'Delegate-Authorization' header for credential degelation.
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
class Security extends WebSecurityConfigurerAdapter {

  @Value("${spring.security.debug:false}")
  private boolean debug = false;

  @Autowired
  private AuthenticationEntryPoint entryPoint;

  @Override
  protected void configure(final HttpSecurity http) throws Exception {
    // @formatter:off
    http
      .authorizeRequests().anyRequest().fullyAuthenticated()
        .and()
      .addFilter(basicAuthenticationFilter())
      .exceptionHandling().authenticationEntryPoint(entryPoint)
        .and()
      .anonymous().principal(Identity.undefined()).authorities(Collections.<GrantedAuthority>singletonList(Role.NONE))
        .and()
      .addFilterAfter(new HttpMethodRestrictionFilter(), AnonymousAuthenticationFilter.class)
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
        .authenticationProvider(authenticationProvider())
    ;
  }

  @Override
  public void configure(final WebSecurity web) throws Exception {
    web.debug(debug);
  }

  private BasicAuthenticationFilter basicAuthenticationFilter() throws Exception {
    final BasicAuthenticationFilter filter =
        new BasicAuthenticationFilter(authenticationManagerBean());
    filter.setAuthenticationDetailsSource(DelegationDetailsSource.usingHeader(AsioSettings.DELEGATE_AUTHORIZATION_HEADER));
    filter.afterPropertiesSet();
    return filter;
  }

  private AuthenticationProvider authenticationProvider() throws Exception {
    final DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setAuthoritiesMapper(ExpandAuthoritiesContainer.instance());
    provider.setUserDetailsService(userDetailsServiceBean());
    provider.afterPropertiesSet();
    return provider;
  }
}

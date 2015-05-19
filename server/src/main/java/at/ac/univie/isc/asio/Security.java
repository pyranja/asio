/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.security.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import javax.servlet.http.HttpServletRequest;

import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class Security {
  private static final Logger log = getLogger(Security.class);

  @Autowired
  private AsioSettings config;

  @Bean
  public AuthenticationDetailsSource<HttpServletRequest, ?> authDetailsSource() {
    return DelegationDetailsSource.usingHeader(config.api.delegateAuthorizationHeader);
  }

  /**
   * Register root user in global user service, and provide a login provider for username/password
   * authentication.
   */
  @Configuration
  static class Authentication extends GlobalAuthenticationConfigurerAdapter {

    @Autowired
    private SecurityProperties security;

    @Override
    public void init(final AuthenticationManagerBuilder auth) throws Exception {
      final SecurityProperties.User root = security.getUser();
      if (root.isDefaultPassword()) {
        log.info(Scope.SYSTEM.marker(), "\n\nroot account: {} - {}\n", root.getName(), root.getPassword());
      }
      auth.inMemoryAuthentication().withUser(root.getName()).password(root.getPassword()).authorities(Role.ADMIN);
    }

    @Override
    public void configure(final AuthenticationManagerBuilder auth) throws Exception {
      auth.authenticationProvider(loginAuthenticationProvider(auth.getDefaultUserDetailsService()));
    }

    private AuthenticationProvider loginAuthenticationProvider(final UserDetailsService userService) throws Exception {
      final DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
      provider.setAuthoritiesMapper(ExpandAuthoritiesContainer.instance());
      provider.setUserDetailsService(userService);
      provider.afterPropertiesSet();
      return provider;
    }
  }

  /**
   * default security settings for rest-ful endpoints
   */
  private static void defaultHttpOptions(final HttpSecurity http) throws Exception {
    http
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
        .requestCache().disable()
        .csrf().disable()
        .logout().disable()
        .headers().cacheControl().contentTypeOptions().xssProtection().frameOptions();
  }

  /**
   * Access rules for the management paths {@code /api/..} and {@code /explore/insight/..}.
   * Protected by basic authentication and access is restricted to authenticated users only,
   * except for {@code ../whoami} requests.
   */
  @Configuration
  @Order(Ordered.HIGHEST_PRECEDENCE + 5)  // apply before other access rules
  static class ApiAccessRules extends WebSecurityConfigurerAdapter {

    @Autowired
    private SecurityProperties security;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
      // @formatter:off
      defaultHttpOptions(http);
      http
          .requestMatchers().antMatchers("/api/**", "/explore/insight/**")
        .and()
          .authorizeRequests()
          .antMatchers("/**/whoami").permitAll()
          .anyRequest().authenticated()
        .and()
          .httpBasic().realmName(security.getBasic().getRealm())
        .and()
          .anonymous().principal(Identity.undefined())
      ;
    // @formatter:on
    }
  }


  /**
   * Datasets are protected by basic authentication, using the global user registrations.
   * <p/>
   * Anonymous users are assigned {@link Role#USER} and therefore have read access to the datasets.
   */
  @Configuration
  @Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
  static class DefaultDatasetAccessRules extends WebSecurityConfigurerAdapter {
    static final int OVERRIDE_DEFAULT_RULES = SecurityProperties.ACCESS_OVERRIDE_ORDER - 2;

    @Autowired
    private SecurityProperties security;

    @Autowired
    private AuthenticationDetailsSource<HttpServletRequest, ?> detailsSource;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
      // @formatter:off
      defaultHttpOptions(http);
      http  // match any request
          .httpBasic().realmName(security.getBasic().getRealm()).authenticationDetailsSource(detailsSource)
        .and()
          .anonymous().principal(Identity.undefined()).authorities(Role.USER.expand())
        .and()
          .addFilterAfter(new HttpMethodRestrictionFilter(), AnonymousAuthenticationFilter.class)
      ;
      // @formatter:on
    }
  }


  /**
   * Grant {@link Role#USER} access level to all dataset requests and support delegated credentials
   * for anonymous requests.
   */
  @Configuration
  @Order(DefaultDatasetAccessRules.OVERRIDE_DEFAULT_RULES)
  @Flock
  static class FlockAccessRules extends WebSecurityConfigurerAdapter {
    @Autowired
    private AuthenticationDetailsSource<HttpServletRequest, ?> detailsSource;

    private final String anonAuthKey = UUID.randomUUID().toString();

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
      // @formatter:off
      defaultHttpOptions(http);
      http  // match any request
          .anonymous().key(anonAuthKey).authenticationFilter(anonymousAuthentication())
        .and()
          .addFilterAfter(new HttpMethodRestrictionFilter(), AnonymousAuthenticationFilter.class)
      ;
      // @formatter:on
    }

    private AnonymousAuthenticationFilter anonymousAuthentication() {
      final AnonymousAuthenticationFilter filter =
          new AnonymousAuthenticationFilter(anonAuthKey, Identity.undefined(), Role.USER.expand());
      filter.setAuthenticationDetailsSource(detailsSource);
      return filter;
    }
  }


  /**
   * Enable uri based authentication or dataset, anonymous access is not allowed, but authentication
   * can be overridden through basic auth.
   */
  @Configuration
  @Order(DefaultDatasetAccessRules.OVERRIDE_DEFAULT_RULES)
  @Brood
  @ConditionalOnProperty(AsioFeatures.VPH_URI_AUTH)
  static class UriBasedDatasetAccessRules extends WebSecurityConfigurerAdapter {
    @Autowired
    private SecurityProperties security;

    @Autowired
    private AuthenticationManager globalAuthenticationManager;
    @Autowired
    private AuthenticationDetailsSource<HttpServletRequest, ?> detailsSource;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
      // @formatter:off
      defaultHttpOptions(http);
      http  // match any request
          .authorizeRequests().anyRequest().authenticated()
        .and()
          .httpBasic().realmName(security.getBasic().getRealm()).authenticationDetailsSource(detailsSource)
        .and()
          .addFilter(uriAuthentication())
          .addFilterAfter(new HttpMethodRestrictionFilter(), AnonymousAuthenticationFilter.class)
      ;
      // @formatter:on
    }

    @Override
    public void configure(final AuthenticationManagerBuilder auth) throws Exception {
      // add provider for uri auth
      auth.authenticationProvider(preAuthenticationProvider())
          .parentAuthenticationManager(globalAuthenticationManager);
    }

    private UriAuthenticationFilter uriAuthentication() throws Exception {
      final UriAuthenticationFilter filter = UriAuthenticationFilter.create();
      filter.setAuthenticationDetailsSource(detailsSource);
      filter.setContinueFilterChainOnUnsuccessfulAuthentication(true);
      filter.setAuthenticationManager(authenticationManagerBean());
      return filter;
    }

    private AuthenticationProvider preAuthenticationProvider() throws Exception {
      final PreAuthenticatedAuthenticationProvider provider =
          new PreAuthenticatedAuthenticationProvider();
      provider.setPreAuthenticatedUserDetailsService(RoleUserService.<PreAuthenticatedAuthenticationToken>create());
      provider.setThrowExceptionWhenTokenRejected(true);
      return provider;
    }
  }


  /**
   * Enable spring security debug mode and logger listeners if configured explicitly.
   */
  @Configuration
  @ConditionalOnProperty("spring.security.debug")
  @Order(Ordered.HIGHEST_PRECEDENCE + 1)
  static class DebugMode extends WebSecurityConfigurerAdapter {
    @Override
    public void init(final WebSecurity web) throws Exception {
      web.debug(true);
    }

    @Bean
    public org.springframework.security.access.event.LoggerListener debugAuthorizationEventLogger() {
      return new org.springframework.security.access.event.LoggerListener();
    }

    @Bean
    public org.springframework.security.authentication.event.LoggerListener debugAuthenticationEventLogger() {
      return new org.springframework.security.authentication.event.LoggerListener();
    }
  }
}

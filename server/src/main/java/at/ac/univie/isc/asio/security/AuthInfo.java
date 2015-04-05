package at.ac.univie.isc.asio.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

/**
 * Aggregate information on a client's authorization.
 */
@AutoValue
public abstract class AuthInfo {
  /**
   * Derive authorization info from the given identity and role.
   *
   * @param login login name used by client
   * @param identity identity of authorized client
   * @param authorities all authorities granted to client
   * @return information on authorization
   */
  public static AuthInfo from(final String login, final Identity identity, final Collection<? extends GrantedAuthority> authorities) {
    final String name = identity.nameOrIfUndefined(null);
    final String secret = identity.isDefined() ? identity.getSecret() : null;
    return create(name, secret, login, AuthorityUtils.authorityListToSet(authorities));
  }

  @JsonCreator
  static AuthInfo create(
      @JsonProperty("name") @Nullable final String name
      , @JsonProperty("secret") @Nullable final String secret
      , @JsonProperty("login") final String login
      , @JsonProperty("authorities") final Iterable<String> authorities
  ) {
    return new AutoValue_AuthInfo(name, secret, login, ImmutableSet.copyOf(authorities));
  }

  AuthInfo() { /* prevent sub-classing */ }

  /**
   * @return name used for delegated authentication if available
   */
  @Nullable
  public abstract String getName();

  /**
   * @return secret used for delegated authentication if available
   */
  @Nullable
  public abstract String getSecret();

  /**
   * @return the login name used for asio
   */
  public abstract String getLogin();

  /**
   * @return all permissions granted based on the {@link Role}
   */
  public abstract Set<String> getAuthorities();
}

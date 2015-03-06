package at.ac.univie.isc.asio.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.springframework.security.core.authority.AuthorityUtils;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Aggregate information on a client's authorization.
 */
@AutoValue
public abstract class AuthInfo {
  /**
   * Derive authorization info from the given identity and role.
   *
   * @param identity identity of authorized client
   * @param role role of authorized client
   * @return information on authorization
   */
  public static AuthInfo from(final Identity identity, final Role role) {
    final String name = identity.nameOrIfUndefined(null);
    final String secret = identity.isDefined() ? identity.getSecret() : null;
    final Set<String> permissions = AuthorityUtils.authorityListToSet(role.getGrantedAuthorities());
    return create(name, secret, role.name(), permissions);
  }

  @JsonCreator
  static AuthInfo create(
      @JsonProperty("name") @Nullable final String name
      , @JsonProperty("secret") @Nullable final String secret
      , @JsonProperty("login") final String login
      , @JsonProperty("permissions") final Iterable<String> permissions
  ) {
    return new AutoValue_AuthInfo(name, secret, login,
        ImmutableSet.copyOf(permissions));
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
  public abstract Set<String> getPermissions();
}

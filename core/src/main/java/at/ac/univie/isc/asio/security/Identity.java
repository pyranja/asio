package at.ac.univie.isc.asio.security;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.security.Principal;

/**
 * Represent a named entity and a secret that proves its identity.
 */
@Immutable
public final class Identity implements Principal {
  /** represents the {@code null} identity */
  private static final Identity UNDEFINED = new Identity(null, null);

  /** Thrown on illegal access to any property of an undefined identity */
  public static final class UndefinedIdentity extends IllegalStateException {
    public UndefinedIdentity() {
      super("use of undefined identity");
    }
  }

  /**
   * Create an identity with given name and secret. Neither name nor secret may be {@code null} to
   * create a valid identity.
   * If either one is null an {@link #undefined() undefined identity} is returned.
   *
   * @param username name of the user
   * @param secret   required api key
   * @return identity object with given
   */
  @Nonnull
  public static Identity from(@Nullable final String username, @Nullable final String secret) {
    if (username == null || secret == null) {
      return Identity.undefined();
    } else {
      return new Identity(username, secret);
    }
  }

  /**
   * Create a dummy identity, that is not associated with any user, e.g. if there is no access
   * control or no valid credentials were given.
   *
   * @return the null identity
   */
  @Nonnull
  public static Identity undefined() {
    return UNDEFINED;
  }

  private final String name;
  private final String secret;

  private Identity(final String name, final String secret) {
    super();
    this.name = name;
    this.secret = secret;
  }

  /**
   * Check if this is a defined identity and return the given fallback if not.
   *
   * @param fallback identity in case this is undefined
   * @return this or given fallback
   */
  @Nullable
  public Identity orIfUndefined(@Nullable final Identity fallback) {
    return isDefined() ? this : fallback;
  }

  /**
   * @throws at.ac.univie.isc.asio.security.Identity.UndefinedIdentity if not valid
   */
  @Override
  @Nonnull
  public String getName() {
    failIfUndefined();
    assert name != null;
    return name;
  }

  /**
   * Return the name of this principal or the given fallback if it is undefined.
   *
   * @param fallback name in case this is undefined
   * @return name or given fallback value
   */
  @Nullable
  public String nameOrIfUndefined(@Nullable final String fallback) {
    return isDefined() ? name : fallback;
  }

  /**
   * The secret that may be used to prove this identity, e.g. a password.
   *
   * @return the secret's value
   */
  @Nonnull
  public String getSecret() {
    failIfUndefined();
    assert secret != null;
    return secret;
  }

  /**
   * @return the user's api key for delegated auth
   */
  @Deprecated
  @Nonnull
  public String getToken() {
    return getSecret();
  }

  private void failIfUndefined() {
    if (!isDefined()) { throw new UndefinedIdentity(); }
  }

  /**
   * Check if this represents actual credentials.
   *
   * @return true if this is a valid representation.
   */
  public boolean isDefined() {
    return this != UNDEFINED;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Identity{");
    if (isDefined()) {
      sb.append("name='").append(name).append('\'');
      sb.append("|secret='[PROTECTED]'");
    } else {
      sb.append("undefined");
    }
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) { return true; }
    if (other == null || getClass() != other.getClass()) { return false; }
    final Identity that = (Identity) other;
    return name.equals(that.name) && secret.equals(that.secret);
  }

  @Override
  public int hashCode() {
    return 31 * name.hashCode() + secret.hashCode();
  }
}
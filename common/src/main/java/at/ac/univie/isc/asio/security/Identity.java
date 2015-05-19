/*
 * #%L
 * asio common
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
package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.AsioError;

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
  private static final Identity UNDEFINED = new Identity("anonymous", null);

  /** Thrown on illegal access to any property of an undefined identity */
  public static final class UndefinedIdentity extends AsioError.Base {
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
   * {@inheritDoc}.
   * <p>
   *   Note: For compatibility with the {@link java.security.Principal} contract, {@code getName}
   *   always returns a non-null {@code String}, even if this identity is undefined.
   *   If the undefined case must be handled explicitly, use {@link #nameOrIfUndefined(String)}.
   * </p>
   * @return name of this identity or the {@code 'anonymous'} if this is undefined.
   */
  @Override
  @Nonnull
  public String getName() {
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
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    final Identity identity = (Identity) o;
    if (name != null ? !name.equals(identity.name) : identity.name != null)
      return false;
    if (secret != null ? !secret.equals(identity.secret) : identity.secret != null)
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (secret != null ? secret.hashCode() : 0);
    return result;
  }
}

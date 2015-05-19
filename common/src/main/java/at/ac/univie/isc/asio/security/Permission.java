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

import at.ac.univie.isc.asio.tool.TypedValue;
import org.springframework.security.core.GrantedAuthority;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Locale;

/**
 * Capabilities, which may be granted to a user.
 */
@Immutable
public final class Permission extends TypedValue<String> implements GrantedAuthority {
  /** matches any permission */
  public static final Permission ANY = new Permission("*");

  // specific permissions
  public static final Permission ACCESS_METADATA = new Permission("ACCESS_METADATA");
  public static final Permission INVOKE_QUERY = new Permission("INVOKE_QUERY");
  public static final Permission INVOKE_UPDATE = new Permission("INVOKE_UPDATE");
  // super user rights
  public static final Permission ACCESS_INTERNALS = new Permission("ACCESS_INTERNALS");
  public static final Permission ADMINISTRATE = new Permission("ADMINISTRATE");

  /** Shared prefix of all Permissions */
  public static final String PREFIX = "PERMISSION_";

  /**
   * Create a permission from given plain name. If not already present,
   * the {@link at.ac.univie.isc.asio.security.Permission#PREFIX shared prefix} is added to it.
   *
   * @param permission the raw name of the permission
   * @return the permission with given name
   */
  public static Permission valueOf(final String permission) {
    return new Permission(permission);
  }

  private Permission(final String name) {
    super(name);
  }

  @Nonnull
  @Override
  protected String normalize(@Nonnull final String val) {
    final String upped = val.toUpperCase(Locale.ENGLISH);
    if (upped.startsWith(PREFIX)) {
      return upped;
    } else {
      return PREFIX + upped;
    }
  }

  /**
   * The name of this permission. Alias to {@link #getAuthority()}.
   * @return permission name
   */
  public String name() {
    return value();
  }

  /**
   * The name of this permission.
   * @return permission name
   */
  @Override
  public String getAuthority() {
    return value();
  }
}

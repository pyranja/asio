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
  public static final Permission ANY = new Permission("*");
  public static final Permission READ = new Permission("READ");
  public static final Permission WRITE = new Permission("WRITE");
  public static final Permission ADMIN = new Permission("ADMIN");
  public static final Permission INVOKE_QUERY = new Permission("INVOKE_QUERY");
  public static final Permission INVOKE_UPDATE = new Permission("INVOKE_UPDATE");

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

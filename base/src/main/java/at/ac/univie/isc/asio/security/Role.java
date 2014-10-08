package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.tool.TypedValue;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Locale;

/**
 * Capabilities, which may be granted to a user.
 */
@Immutable
public final class Role extends TypedValue<String> {
  public static final Role ANY = new Role("*");
  public static final Role READ = new Role("READ");
  public static final Role WRITE = new Role("WRITE");
  public static final Role ADMIN = new Role("ADMIN");

  public static Role valueOf(final String role) {
    return new Role(role);
  }

  private Role(final String name) {
    super(name);
  }

  @Nonnull
  @Override
  protected String normalize(@Nonnull final String val) {
    return val.toUpperCase(Locale.ENGLISH);
  }

  public String name() {
    return value();
  }
}

package at.ac.univie.isc.asio.security;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A Permission represents a bundle of granted Roles.
 */
public enum Permission {

  NONE(ImmutableSet.<Role>of()),
  READ(ImmutableSet.of(Role.READ)),
  FULL(ImmutableSet.of(Role.READ, Role.WRITE)),
  ADMIN(ImmutableSet.of(Role.READ, Role.WRITE, Role.ADMIN));

  private static final Map<String, Permission> LOOKUP;
  static {
    final ImmutableMap.Builder<String, Permission> builder = ImmutableMap.builder();
    for (Permission each : Permission.values()) {
      builder.put(each.name(), each);
    }
    LOOKUP = builder.build();
  }

  public static Permission parse(@Nullable final String name) {
    if (name == null) { return Permission.NONE; }
    final Permission found = LOOKUP.get(name.toUpperCase(Locale.ENGLISH));
    return found != null ? found : Permission.NONE;
  }

  private final Set<Role> roles;

  private Permission(final Set<Role> roles) {
    this.roles = roles;
  }

  public boolean grants(final Role role) {
    requireNonNull(role);
    return roles.contains(role) || Role.ANY.equals(role);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this.name())
        .add("roles", roles)
        .toString();
  }
}

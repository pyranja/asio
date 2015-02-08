package at.ac.univie.isc.asio.security;

import com.google.common.base.Optional;

import javax.ws.rs.core.SecurityContext;

import static java.util.Objects.requireNonNull;

/**
 * Static holder of a thread-scoped {@code SecurityContext}.
 */
// FIXME : remove when spring security is available
public final class SecurityContextHolder {
  private static final ThreadLocal<SecurityContext> holder = new ThreadLocal<>();

  public static Optional<SecurityContext> get() {
    return Optional.fromNullable(holder.get());
  }

  public static void set(final SecurityContext context) {
    holder.set(requireNonNull(context));
  }

  public static void clear() {
    holder.remove();
  }
}

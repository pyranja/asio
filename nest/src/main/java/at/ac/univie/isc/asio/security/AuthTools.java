package at.ac.univie.isc.asio.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

/**
 * Tools for working with spring security.
 */
public final class AuthTools {
  private AuthTools() { /* no instances */ }

  /**
   * Extract the {@link Identity} from the authentication's credentials i one is present.
   *
   * @param context spring security context
   * @return the client's identity if one is present
   */
  @Nonnull
  public static Identity findIdentity(@Nonnull final SecurityContext context) {
    requireNonNull(context, "spring security context");
    final Authentication authentication = context.getAuthentication();
    if (authentication != null && authentication.getCredentials() instanceof Identity) {
      return (Identity) authentication.getCredentials();
    }
    return Identity.undefined();
  }
}

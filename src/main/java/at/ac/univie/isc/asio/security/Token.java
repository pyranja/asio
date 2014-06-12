package at.ac.univie.isc.asio.security;

import com.google.common.base.Objects;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.security.Principal;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Objects.requireNonNull;

/**
 * An entity and associated credentials for delegation.
 */
@Immutable
public final class Token implements Principal {
  public static final String UNKNOWN_PRINCIPAL = "anonymous";
  public static final Token ANONYMOUS = new Token(UNKNOWN_PRINCIPAL, "");

  /**
   * @param username optional name of the user
   * @param token required api key
   * @return token object representing the user credentials
   */
  public static Token from(@Nullable final String username, final String token) {
    if (nullToEmpty(username).isEmpty()) {
      return new Token(UNKNOWN_PRINCIPAL, token);
    } else {
      return new Token(username, token);
    }
  }

  private final String name;
  private final String token;

  private Token(final String name, final String token) {
    super();
    this.token = requireNonNull(token);
    this.name = requireNonNull(name);
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * @return the user's api key for delegated auth
   */
  public String getToken() {
    return token;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("name", name)
        .toString();
  }
}

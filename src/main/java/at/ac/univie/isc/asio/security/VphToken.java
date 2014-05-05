package at.ac.univie.isc.asio.security;

import com.google.common.base.Objects;

import javax.security.auth.Destroyable;
import java.security.Principal;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Objects.requireNonNull;

public class VphToken implements Principal, Destroyable {

  public static final String UNKNOWN_PRINCIPAL = "anonymous";

  public static VphToken from(final String username, final char[] password) {
    if (nullToEmpty(username).isEmpty()) {
      return new VphToken(UNKNOWN_PRINCIPAL, password);
    } else {
      return new VphToken(username, password);
    }
  }

  private final char[] token;
  private final String name;

  private volatile boolean valid = true;

  private VphToken(final String name, final char[] password) {
    super();
    token = requireNonNull(password);
    this.name = requireNonNull(name);
  }

  @Override
  public String getName() {
    return name;
  }

  public char[] getToken() {
    checkState(valid, "token already destroyed");
    return token;
  }

  @Override
  public void destroy() {
    for (int i = 0; i < token.length; i++) {
      token[i] = ' ';
    }
    valid = false;
  }

  @Override
  public boolean isDestroyed() {
    return !valid;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("name", name)
        .add("valid", valid)
        .toString();
  }
}

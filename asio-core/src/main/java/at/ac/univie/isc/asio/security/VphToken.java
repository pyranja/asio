package at.ac.univie.isc.asio.security;

import static com.google.common.base.Preconditions.checkState;

import java.security.Principal;

import javax.security.auth.Destroyable;

public class VphToken implements Principal, Destroyable {

  private final char[] token;

  private volatile boolean valid = true;

  public VphToken(final char[] password) {
    super();
    token = password;
  }

  // TODO extract from token ?
  @Override
  public String getName() {
    return "<unknown>";
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
    return String.format("VphToken [valid=%s]", valid);
  }
}

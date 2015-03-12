package at.ac.univie.isc.asio.security;

import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * noop translator
 */
public final class NoTranslation implements TranslateAuthorization {
  private NoTranslation() {}

  public static NoTranslation create() {
    return new NoTranslation();
  }

  @Override
  public Wrapped translate(final GrantedAuthority ignored, final HttpServletRequest request, final HttpServletResponse response) {
    return Wrapped.create(request, response);
  }

  @Override
  public String toString() {
    return "NoTranslation{}";
  }
}

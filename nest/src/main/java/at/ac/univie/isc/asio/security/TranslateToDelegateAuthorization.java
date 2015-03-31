package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.AsioSettings;
import com.google.common.base.Converter;
import org.apache.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Copy the basic auth header from source request to the 'Delegate-Authorization' header and use
 * the authority as login.
 */
public final class TranslateToDelegateAuthorization implements TranslateAuthorization {
  private final Converter<Identity, String> formatter;
  private final String secret;

  /**
   * Create a translator, that uses the given secret as basic auth password.
   * @param secret internal login password
   * @return initialized translator
   */
  public static TranslateToDelegateAuthorization withSecret(final String secret) {
    return new TranslateToDelegateAuthorization(secret, BasicAuthConverter.fromIdentity());
  }

  private TranslateToDelegateAuthorization(final String secret, final Converter<Identity, String> formatter) {
    this.secret = secret;
    this.formatter = formatter;
  }

  @Override
  public Wrapped translate(final GrantedAuthority authority, final HttpServletRequest request, final HttpServletResponse response) {
    final WithHeadersRequest translatedRequest = new WithHeadersRequest(request);
    // override authorization with internal login
    final Identity login = Identity.from(authority.getAuthority(), secret);
    translatedRequest.override(HttpHeaders.AUTHORIZATION, formatter.convert(login));
    // if present use original authorization as delegated credentials
    final String delegated = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (delegated != null) {
      translatedRequest.override(AsioSettings.DELEGATE_AUTHORIZATION_HEADER, delegated);
    }
    return Wrapped.create(translatedRequest, response);
  }

  @Override
  public String toString() {
    return "TranslateToDelegateAuthorization{}";
  }

  /**
   * Allow to add headers to a http request. Added headers take precedence over headers of original
   * request.
   */
  static class WithHeadersRequest extends HttpServletRequestWrapper {
    private final Map<String, String> localHeaders = new HashMap<>(5);

    private WithHeadersRequest(final HttpServletRequest request) {
      super(request);
    }

    WithHeadersRequest override(final String name, final String value) {
      // normalize header names for case insensitive matching
      this.localHeaders.put(name.toLowerCase(Locale.ENGLISH), value);
      return this;
    }

    @Override
    public String getHeader(final String name) {
      final String local = localHeaders.get(name.toLowerCase(Locale.ENGLISH));
      return local == null ? super.getHeader(name) : local;
    }

    @Override
    public Enumeration<String> getHeaders(final String name) {
      final String local = localHeaders.get(name.toLowerCase(Locale.ENGLISH));
      return local == null ? super.getHeaders(name) : Collections.enumeration(Collections.singleton(local));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
      final List<String> names = new ArrayList<>(localHeaders.keySet());
      names.addAll(Collections.list(super.getHeaderNames()));
      return Collections.enumeration(names);
    }
  }
}

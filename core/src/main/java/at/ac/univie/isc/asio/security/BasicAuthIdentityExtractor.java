package at.ac.univie.isc.asio.security;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse a HTTP "Authorization" header string for an {@link at.ac.univie.isc.asio.security.Identity}.
 * <p>
 * The header <strong>MUST</strong> be formatted according to the Basic Authentication scheme.
 * A password <strong>MUST</strong> be present, but the username <strong>MAY</strong> be omitted.
 * </p>
 */
@ThreadSafe
public final class BasicAuthIdentityExtractor {
  private final static Pattern BASIC_AUTH_PATTERN = Pattern.compile("^Basic ([A-Za-z0-9+/=]*)$");
  private final static Pattern CREDENTIALS_PATTERN = Pattern.compile("^([^:]*):(.*)$");

  /**
   * @param auth Authorization header value
   * @return extracted identity
   * @throws BasicAuthIdentityExtractor.MalformedAuthHeader if parsing the header fails
   */
  @Nonnull
  public Identity authenticate(@Nullable final String auth) throws MalformedAuthHeader {
    if (auth == null || auth.isEmpty()) {
      return Identity.undefined();
    }
    final String encodedCredentials = readBasicAuthCredentials(auth);
    final CharBuffer credentials = decode(encodedCredentials);
    return parse(credentials);
  }

  private CharBuffer decode(final String encodedCredentials) {
    final byte[] raw = BaseEncoding.base64().decode(encodedCredentials);
    return Charsets.UTF_8.decode(ByteBuffer.wrap(raw));
  }

  private Identity parse(final CharBuffer credentials) {
    final Matcher match = CREDENTIALS_PATTERN.matcher(credentials);
    if (match.matches()) {
      final String username = match.group(1);
      final String password = match.group(2);
      return Identity.from(username, password);
    } else {
      throw new MalformedAuthHeader("illegal credentials format");
    }
  }

  private String readBasicAuthCredentials(final String auth) {
    final Matcher match = BASIC_AUTH_PATTERN.matcher(auth);
    if (match.matches()) {
      return match.group(1);
    } else {
      throw new MalformedAuthHeader("unsupported auth scheme : expected BasicAuthentication");
    }
  }

  public static class MalformedAuthHeader extends IllegalArgumentException {
    public MalformedAuthHeader(final String reason) {
      super(reason);
    }
  }
}

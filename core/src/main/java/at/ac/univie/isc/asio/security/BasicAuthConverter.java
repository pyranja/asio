package at.ac.univie.isc.asio.security;

import com.google.common.base.Charsets;
import com.google.common.base.Converter;
import com.google.common.io.BaseEncoding;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convert between credentials formatted according to the Basic Authentication Scheme (RFC-2617).
 * <p>
 * {@code String} input <strong>MUST</strong> be formatted according to the Basic Authentication
 * Scheme. Both username and password <strong>MAY</strong> be empty.
 * </p>
 * <p>
 * {@code Identity} input <strong>MUST</strong> be {@link Identity#isDefined() defined}.
 * </p>
 * <p>
 * As defined by {@link com.google.common.base.Converter} {@code null} input will always yield
 * {@code null} output.
 * </p>
 *
 * @see <a href='https://www.ietf.org/rfc/rfc2617.txt'>RFC 2617</a>
 */
@ThreadSafe
public final class BasicAuthConverter extends Converter<String, Identity> {
  private final static Pattern BASIC_AUTH_PATTERN = Pattern.compile("^Basic ([A-Za-z0-9+/=]*)$");
  private final static Pattern CREDENTIALS_PATTERN = Pattern.compile("^([^:]*):(.*)$");
  /**
   * Conservative limit on the maximal allowed length of a username:password pair.
   * This bounds the raw size of the pair at 64KiB (if all characters must be encoded as a 4 octet
   * code point in utf-8. For the common ASCII case the maximal allowed size is therefore ~ 16 KiB.
   * This is well above the limits applied by widely used http servers (between 4KiB - 16KiB).
   */
  public static final int MAXIMUM_CREDENTIALS_LENGTH = 16 * 1_024;

  private static final BasicAuthConverter INSTANCE = new BasicAuthConverter();

  BasicAuthConverter() {}

  public static Converter<String,Identity> fromString() {
    return INSTANCE;
  }

  public static Converter<Identity, String> fromIdentity() {
    return INSTANCE.reverse();
  }

  @Override
  protected Identity doForward(@Nonnull final String input) throws MalformedCredentials {
    final String encodedCredentials = readBasicAuthCredentials(input);
    final CharBuffer credentials = decode(encodedCredentials);
    return parse(credentials);
  }

  @Override
  protected String doBackward(@Nonnull final Identity identity) throws MalformedCredentials, Identity.UndefinedIdentity {
    final CharBuffer credentials = formatCredentials(identity.getSecret(), identity.getName());
    return "Basic " + encode(credentials);
  }

  private String encode(final CharBuffer usernameAndPassword) {
    final ByteBuffer raw = Charsets.UTF_8.encode(usernameAndPassword);
    assert raw.hasArray() : "encoding result buffer is not array backed";
    return BaseEncoding.base64()
        .encode(raw.array(), raw.arrayOffset(), raw.arrayOffset() + raw.limit());
  }

  private CharBuffer formatCredentials(final String password, final String username) {
    final int size = username.length() + password.length() + 1;
    assert size < MAXIMUM_CREDENTIALS_LENGTH
        : "cannot encode identity - exceeds maximal credential size";
    final CharBuffer usernameAndPassword = CharBuffer.allocate(size);
    checkedPut(username, usernameAndPassword).put(':').put(password).rewind();
    return usernameAndPassword;
  }

  private CharBuffer checkedPut(final String username, final CharBuffer buffer) {
    for (int i = 0; i < username.length(); i++) {
      final char current = username.charAt(i);
      if (current == ':') {
        throw new MalformedCredentials("found illegal character ':' in <" + username + ">");
      }
      buffer.put(current);
    }
    return buffer;
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
      throw new MalformedCredentials("illegal credentials format");
    }
  }

  private String readBasicAuthCredentials(final String auth) {
    final Matcher match = BASIC_AUTH_PATTERN.matcher(auth);
    if (match.matches()) {
      return match.group(1);
    } else {
      throw new MalformedCredentials("unsupported auth scheme - expected 'Basic Authentication'");
    }
  }

  /**
   * Thrown if the given credentials cannot be converted, e.g. it is not the expected scheme on
   * parsing or it contains illegal characters during formatting.
   */
  public static final class MalformedCredentials extends IllegalArgumentException {
    public MalformedCredentials(final String reason) {
      super(reason);
    }
  }
}

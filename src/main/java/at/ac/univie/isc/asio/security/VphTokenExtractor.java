package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.DatasetUsageException;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.io.BaseEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VphTokenExtractor {

  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(VphTokenExtractor.class);

  private final static Pattern BASIC_AUTH_PATTERN = Pattern.compile("^Basic ([A-Za-z0-9+/=]*)$");
  private final static Pattern CREDENTIALS_PATTERN = Pattern.compile("^([^:]*):(.*)$");

  public Principal authenticate(final HttpHeaders headers) {
    final Optional<String> auth = findAuthentication(headers);
    if (!auth.isPresent()) {
      return Anonymous.INSTANCE;
    }
    final String encodedCredentials = readBasicAuthCredentials(auth.get());
    final CharBuffer credentials = decode(encodedCredentials);
    return parse(credentials);
  }

  private CharBuffer decode(final String encodedCredentials) {
    final byte[] raw = BaseEncoding.base64().decode(encodedCredentials);
    return Charsets.UTF_8.decode(ByteBuffer.wrap(raw));
  }

  private VphToken parse(final CharBuffer credentials) {
    final Matcher match = CREDENTIALS_PATTERN.matcher(credentials);
    if (match.matches()) {
      final String username = match.group(1);
      final char[] password = match.group(2).toCharArray();
      return VphToken.from(username, password);
    } else {
      throw new DatasetUsageException("illegal credentials format");
    }
  }

  private String readBasicAuthCredentials(final String auth) {
    final Matcher match = BASIC_AUTH_PATTERN.matcher(auth);
    if (match.matches()) {
      return match.group(1);
    } else {
      throw new DatasetUsageException("unsupported auth scheme : expected BasicAuthentication");
    }
  }

  private Optional<String> findAuthentication(final HttpHeaders headers) {
    final List<String> authHeader = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || authHeader.isEmpty()) {
      return Optional.absent();
    } else if (authHeader.size() == 1) {
      return Optional.of(Iterables.getOnlyElement(authHeader));
    } else {
      log.warn("illegal authentication header {}", authHeader);
      throw new DatasetUsageException("illegal access credentials");
    }
  }
}

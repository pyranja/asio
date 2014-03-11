package at.ac.univie.isc.asio.tool;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.io.CharStreams;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import javax.ws.rs.core.Response;

/**
 * Monitors test execution and attempts to log the received JAX-RS response if available on the set
 * client provider.
 *
 * Created with IntelliJ IDEA. User: borck_000 ; Date: 1/27/14 ; Time: 8:00 PM
 */
public class ResponseMonitor extends TestWatcher {

  private final static Logger log = LoggerFactory.getLogger(ResponseMonitor.class);

  private Supplier<WebClient> provider;

  public ResponseMonitor(final Supplier<WebClient> provider) {
    this.provider = provider;
  }

  @Override
  protected void failed(final Throwable e, final Description description) {
    String responseText = "[no response body available]";
    Optional<Response> response = fetchResponse();
    if (response.isPresent()) {
      responseText = stringify(response.get());
    }
    if (e instanceof AssertionError) {
      log.error("{} failed expectation {} \nreceived response :\n{}", description,
                e.getMessage(), responseText);
    } else {
      log.error("{} failed with internal error {} \nreceived response :\n{}", description,
                e.getMessage(), responseText, e);
    }
  }

  private Optional<Response> fetchResponse() {
    WebClient client = provider.get();
    if (client != null) {
      return Optional.fromNullable(client.getResponse());
    }
    return Optional.absent();
  }

  /**
   * @param response to be printed
   * @return the header and body of this response as text
   */
  private String stringify(final Response response) {
    final StringBuilder sb = new StringBuilder();
    sb.append("STATUS ").append(stringify(response.getStatusInfo()));
    sb.append("\nHEADER\n").append(response.getStringHeaders());
    sb.append("\nBODY\n");
    appendEntity(response.getEntity(), sb);
    sb.append("\nEND RESPONSE");
    return sb.toString();
  }

  /**
   * append the given response entity to a StringBuilder. If the entity is an InputStream the stream
   * is consumed and copied to the builder.
   *
   * @param entity to print
   * @param to builder to hold text data
   * @return the given builder
   */
  private StringBuilder appendEntity(final Object entity, final StringBuilder to) {
    if (entity instanceof InputStream) {
      try (InputStreamReader body = new InputStreamReader((InputStream) entity, Charsets.UTF_8)) {
        CharStreams.copy(body, to);
      } catch (final IOException e) {
        to.append("\n!IO error on reading body!\n").append(e.getLocalizedMessage());
      }
    } else {
      to.append(entity);
    }
    return to;
  }

  /**
   * @param status to print
   * @return status as text in format [status_code|status_class : reason]
   */
  private String stringify(final Response.StatusType status) {
    return String.format(Locale.ENGLISH, "[%s|%s : %s]", status.getStatusCode(),
                         status.getFamily(), status.getReasonPhrase());
  }
}

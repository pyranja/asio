package at.ac.univie.isc.asio.jaxrs;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.DatasetUsageException;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import java.util.Locale;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.compatibleTo;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class DatasetExceptionMapperTest {
  private Request request = mock(Request.class);
  private final DatasetExceptionMapper subject = new DatasetExceptionMapper(false);

  @Test
  public void ignore_accept_and_use_json_as_format() throws Exception {
    final Variant xmlVariant = new Variant(MediaType.APPLICATION_XML_TYPE, Locale.ENGLISH, null);
    when(request.selectVariant(anyList())).thenReturn(xmlVariant);
    final DatasetException failure = new DatasetFailureException(new IllegalStateException("test"));
    final Response response = subject.toResponse(failure);
    assertThat(response.getMediaType(), is(compatibleTo(MediaType.APPLICATION_JSON_TYPE)));
  }

  @Test
  public void use_json_as_default() throws Exception {
    final DatasetException failure = new DatasetFailureException(new IllegalStateException("test"));
    final Response response = subject.toResponse(failure);
    assertThat(response.getMediaType(), is(compatibleTo(MediaType.APPLICATION_JSON_TYPE)));
  }

  @Test
  public void send_500_for_failures() throws Exception {
    final DatasetException failure = new DatasetFailureException(new IllegalStateException("test"));
    final Response response = subject.toResponse(failure);
    assertThat(response, hasStatus(Response.Status.INTERNAL_SERVER_ERROR));
  }

  @Test
  public void send_400_for_user_errors() throws Exception {
    final DatasetException usage = new DatasetUsageException("test");
    final Response response = subject.toResponse(usage);
    assertThat(response, hasStatus(Response.Status.BAD_REQUEST));
  }
}

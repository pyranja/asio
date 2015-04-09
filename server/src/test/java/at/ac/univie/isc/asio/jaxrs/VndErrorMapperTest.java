package at.ac.univie.isc.asio.jaxrs;

import at.ac.univie.isc.asio.*;
import at.ac.univie.isc.asio.engine.TypeMatchingResolver;
import at.ac.univie.isc.asio.insight.Correlation;
import at.ac.univie.isc.asio.insight.Emitter;
import at.ac.univie.isc.asio.insight.VndError;
import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.compatibleTo;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VndErrorMapperTest {
  private final Emitter emitter = Mockito.mock(Emitter.class);
  private final VndErrorMapper subject = new VndErrorMapper(emitter);

  @Before
  public void mockEmitter() {
    final VndError error = VndError.from(new RuntimeException(), Correlation.valueOf("test"), -1L, false);
    when(emitter.emit(any(Exception.class))).thenReturn(error);
  }

  @Test
  public void should_send_status__INTERNAL_SERVER_ERROR__as_default() throws Exception {
    final RuntimeException failure = new RuntimeException("test");
    assertThat(subject.toResponse(failure), hasStatus(Response.Status.INTERNAL_SERVER_ERROR));
  }

  @Test
  public void should_send_status__BAD_REQUEST__for_InvalidUsage_exceptions() throws Exception {
    class UsageError extends InvalidUsage {
      public UsageError() {
        super("test");
      }
    }
    final RuntimeException failure = new UsageError();
    assertThat(subject.toResponse(failure), hasStatus(Response.Status.BAD_REQUEST));
  }

  @Test
  public void should_send_status_from_WebApplicationException() throws Exception {
    final WebApplicationException failure =
        new WebApplicationException("test", Response.Status.PAYMENT_REQUIRED);
    assertThat(subject.toResponse(failure), hasStatus(Response.Status.PAYMENT_REQUIRED));
  }

  @Test
  public void should_send_mapped_status_for_known_exceptions() throws Exception {
    final List<MediaType> noMediaTypes = Collections.emptyList();
    final ImmutableList<Exception> knownErrors = ImmutableList.<Exception>builder()
        .add(new Language.NotSupported(Language.valueOf("test")))
        .add(new Id.NotFound(Id.valueOf("test")))
        .add(new TypeMatchingResolver.NoMatchingFormat(noMediaTypes, noMediaTypes))
        .add(new AccessDeniedException("test"))
        .build();
    for (final Exception failure : knownErrors) {
      final Response.Status expected = VndErrorMapper.ERROR_CODES.get(failure.getClass());
      assertThat(failure.getClass().getName() + " not mapped to expected status",
          subject.toResponse(failure), hasStatus(expected));
    }
  }

  @Test
  public void should_send_json_response() throws Exception {
    final Response response = subject.toResponse(new RuntimeException("test"));
    assertThat(response.getMediaType(), compatibleTo(Mime.VND_ERROR.type()));
  }

  @Test
  public void should_send__Error__instance_as_response_entity() throws Exception {
    final Response response = subject.toResponse(new RuntimeException("test"));
    assertThat(response.getEntity(), instanceOf(VndError.class));
  }

  @Test
  public void should_emit_error_event() throws Exception {
    final RuntimeException failure = new RuntimeException("test");
    subject.toResponse(failure);
    verify(emitter).emit(failure);
  }

  @Test
  public void should_use_VndError_from_emitter_as_entity() throws Exception {
    final RuntimeException failure = new RuntimeException("test");
    final VndError error = VndError.from(failure, Correlation.valueOf("test"), -1L, false);
    given(emitter.emit(failure)).willReturn(error);
    assertThat(subject.toResponse(failure).getEntity(), Matchers.<Object>sameInstance(error));
  }
}

package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.MockFormat;
import at.ac.univie.isc.asio.jaxrs.AsyncResponseFake;
import at.ac.univie.isc.asio.tool.Payload;
import at.ac.univie.isc.asio.transport.ObservableStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasFamily;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class OperationObserverTest {

  @Rule
  public ExpectedException error = ExpectedException.none();

  private AsyncResponseFake async_context;
  private OperationObserver subject;
  private Response.ResponseBuilder response;

  @Before
  public void setup() {
    async_context = spy(new AsyncResponseFake());
    response = Response.ok().type(MockFormat.APPLICABLE_CONTENT_TYPE);
    subject = new OperationObserver(async_context, response);
  }

  @Test
  public void should_resume_with_exception_on_error() throws Exception {
    final Throwable failure = new IllegalStateException("TEST");
    Observable.<ObservableStream>error(failure).subscribe(subject);
    assertThat(async_context.error(), is(failure));
  }

  @Test
  public void should_resume_with_success_on_next() throws Exception {
    observable(Payload.randomWithLength(100)).subscribe(subject);
    assertThat(async_context.response(), hasFamily(Response.Status.Family.SUCCESSFUL));
  }

  @Test
  public void should_respond_with_observable_stream() throws Exception {
    observable(Payload.randomWithLength(100)).subscribe(subject);
    assertThat(async_context.response().getEntity(), is(instanceOf(ObservableStream.class)));
  }

  @Test
  public void should_use_provided_content_type() throws Exception {
    response.type(MockFormat.NOT_APPLICABLE_CONTENT_TYPE);
    observable(Payload.randomWithLength(100)).subscribe(subject);
    assertThat(async_context.response().getMediaType(), is(equalTo(MockFormat.NOT_APPLICABLE_CONTENT_TYPE)));
  }

  @Test
  public void should_resume_with_no_content_if_observable_empty() throws Exception {
    Observable.<ObservableStream>empty().subscribe(subject);
    assertThat(async_context.response(), hasStatus(Response.Status.NO_CONTENT));
  }

  @Test
  public void should_not_resume_if_not_suspended() throws Exception {
    final Object dummy = new Object();
    async_context.resume(dummy);
    observable(Payload.randomWithLength(100)).subscribe(subject);
    verify(async_context).resume(dummy);
  }

  @Test
  public void should_not_resume_with_error_if_not_suspended() throws Exception {
    async_context.cancel();
    Observable.<ObservableStream>error(new IllegalStateException("TEST")).subscribe(subject);
    verify(async_context, never()).resume(Mockito.any(Throwable.class));
  }

  private Observable<ObservableStream> observable(final byte[] payload) {
    return Observable.just(ObservableStream.from(new ByteArrayInputStream(payload)));
  }
}

package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.jaxrs.AsyncResponseFake;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import rx.Observable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasFamily;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class CommandObserverTest {

  @Rule
  public ExpectedException error = ExpectedException.none();

  private Command.Results results = mock(Command.Results.class);
  private AsyncResponseFake async_context;
  private CommandObserver subject;

  @Before
  public void setup() {
    async_context = spy(new AsyncResponseFake());
    subject = new CommandObserver(async_context);
  }

  @Test
  public void should_resume_with_wrapped_exception_on_error() throws Exception {
    final Throwable failure = new IllegalStateException("TEST");
    Observable.<Command.Results>error(failure).subscribe(subject);
    assertThat(async_context.error(), instanceOf(DatasetException.class));
    assertThat(async_context.error().getCause(), is(failure));
  }

  @Test
  public void should_resume_with_success_on_next() throws Exception {
    observable().subscribe(subject);
    assertThat(async_context.response(), hasFamily(Response.Status.Family.SUCCESSFUL));
  }

  @Test
  public void should_respond_with_observable_stream() throws Exception {
    observable().subscribe(subject);
    assertThat(async_context.response().getEntity(), is((Object) results));
  }

  @Test
  public void should_use_provided_content_type() throws Exception {
    final MediaType expectedMime = MediaType.valueOf("test/test");
    when(results.format()).thenReturn(expectedMime);
    observable().subscribe(subject);
    assertThat(async_context.response().getMediaType(), is(equalTo(expectedMime)));
  }

  @Test
  public void should_resume_with_no_content_if_observable_empty() throws Exception {
    Observable.<Command.Results>empty().subscribe(subject);
    assertThat(async_context.response(), hasStatus(Response.Status.NO_CONTENT));
  }

  @Test
  public void should_not_resume_if_not_suspended() throws Exception {
    final Object dummy = new Object();
    async_context.resume(dummy);
    observable().subscribe(subject);
    verify(async_context).resume(dummy);
  }

  @Test
  public void should_not_resume_with_error_if_not_suspended() throws Exception {
    async_context.cancel();
    Observable.<Command.Results>error(new IllegalStateException("TEST")).subscribe(subject);
    verify(async_context, never()).resume(Mockito.any(Throwable.class));
  }

  private Observable<Command.Results> observable() {
    return Observable.just(results);
  }
}

package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.jaxrs.AsyncResponseFake;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import rx.Observable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.OutputStream;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasFamily;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class SendResultsTest {

  @Rule
  public ExpectedException error = ExpectedException.none();

  private StreamedResults results = new StreamedResults(MediaType.APPLICATION_JSON_TYPE) {
    @Override
    protected void doWrite(final OutputStream output) throws IOException {}
  };
  private AsyncResponseFake async = AsyncResponseFake.create();
  private SendResults subject = SendResults.to(async);

  @Test
  public void should_resume_with_wrapped_exception_on_error() throws Exception {
    final Throwable failure = new IllegalStateException("TEST");
    Observable.<StreamedResults>error(failure).subscribe(subject);
    assertThat(async.error(), instanceOf(DatasetException.class));
    assertThat(async.error().getCause(), is(failure));
  }

  @Test
  public void should_resume_with_success_on_next() throws Exception {
    observable().subscribe(subject);
    assertThat(async.response(), hasFamily(Response.Status.Family.SUCCESSFUL));
  }

  @Test
  public void should_respond_with_observed_stream() throws Exception {
    observable().subscribe(subject);
    assertThat(async.response().getEntity(), is((Object) results));
  }

  @Test
  public void should_use_provided_content_type() throws Exception {
    final MediaType expectedMime = MediaType.valueOf("application/json");
    observable().subscribe(subject);
    assertThat(async.response().getMediaType(), is(equalTo(expectedMime)));
  }

  @Test
  public void should_resume_with_no_content_if_observable_empty() throws Exception {
    Observable.<StreamedResults>empty().subscribe(subject);
    assertThat(async.response(), hasStatus(Response.Status.NO_CONTENT));
  }

  @Test
  public void should_not_resume_if_not_suspended() throws Exception {
    final AsyncResponseFake completedAsync = AsyncResponseFake.completed();
    final SendResults sender = SendResults.to(completedAsync);
    observable().subscribe(sender);
    assertThat(completedAsync.timesResumed(), is(0));
  }

  @Test
  public void should_not_resume_with_error_if_not_suspended() throws Exception {
    final AsyncResponseFake completedAsync = AsyncResponseFake.completed();
    final SendResults sender = SendResults.to(completedAsync);
    Observable.<StreamedResults>error(new IllegalStateException("TEST")).subscribe(sender);
    assertThat(completedAsync.timesResumed(), is(0));
  }

  private Observable<StreamedResults> observable() {
    return Observable.just(results);
  }
}

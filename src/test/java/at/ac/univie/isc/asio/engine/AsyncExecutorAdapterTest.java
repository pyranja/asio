package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.*;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;

import java.io.ByteArrayOutputStream;

import static at.ac.univie.isc.asio.MockOperations.dummy;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author pyranja
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncExecutorAdapterTest {

  @Rule
  public ExpectedException error = ExpectedException.none();
  @Rule
  public Timeout timeout = new Timeout(2000);

  private AsyncExecutorAdapter subject;
  @Mock
  private AsyncExecutor adapted;
  private Observable<Command.Results> obs;

  @Before
  public void setup() {
    subject = new AsyncExecutorAdapter(adapted, MoreExecutors.sameThreadExecutor());
  }

  @Test
  public void should_forward_operation() throws Exception {
    subject.execute(dummy());
    verify(adapted).accept(dummy());
  }

  @Test
  public void should_reject_null_operation() throws Exception {
    error.expect(NullPointerException.class);
    subject.execute(null);
  }

  @Test
  public void should_not_rethrow_adapted_errors() throws Exception {
    doThrow(new MockDatasetException()).when(adapted).accept(any(DatasetOperation.class));
    obs = subject.execute(dummy());
  }

  @Test
  public void should_return_failing_observable_if_adapted_throws() throws Exception {
    doThrow(new MockDatasetException()).when(adapted).accept(any(DatasetOperation.class));
    obs = subject.execute(dummy());
    error.expect(DatasetException.class);
    obs.toBlocking().single();
  }

  @Test
  public void should_return_failing_observable_if_future_fails() throws Exception {
    final MockDatasetException failure = new MockDatasetException();
    doReturn(Futures.immediateFailedFuture(failure)).when(adapted).accept(any(DatasetOperation.class));
    obs = subject.execute(dummy());
    error.expect(DatasetException.class);
    obs.toBlocking().single();
  }

  @Test
  public void should_return_stream_with_payload_from_future() throws Exception {
    doReturn(MockResult.successFuture()).when(adapted).accept(any(DatasetOperation.class));
    obs = subject.execute(dummy());
    final ByteArrayOutputStream sink = new ByteArrayOutputStream();
    obs.toBlocking().single().write(sink);
    assertThat(sink.toByteArray(), is(equalTo(MockResult.PAYLOAD)));
  }
}

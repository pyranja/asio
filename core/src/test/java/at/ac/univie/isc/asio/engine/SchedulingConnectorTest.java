package at.ac.univie.isc.asio.engine;

import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class SchedulingConnectorTest {
  public static final Parameters NULL_PARAMS = ParametersBuilder.with(Language.SQL).build();
  public static final StreamedResults DUMMY_RESULTS = new StreamedResults(MediaType.WILDCARD_TYPE) {
    @Override
    protected void doWrite(final OutputStream output) throws IOException {

    }
  };

  private final Connector delegate = Mockito.mock(Connector.class);
  private final SchedulingConnector subject =
      SchedulingConnector.around(Schedulers.newThread(), delegate);

  @Test
  public void wrapped_results_should_not_be_altered() throws Exception {
    when(delegate.accept(NULL_PARAMS)).thenReturn(Observable.just(DUMMY_RESULTS));
    final StreamedResults results = subject.accept(NULL_PARAMS).toBlocking().single();
    assertThat(results, sameInstance(DUMMY_RESULTS));
  }

  @Test
  public void should_subscribe_on_set_scheduler() throws Exception {
    when(delegate.accept(NULL_PARAMS)).thenReturn(Observable.<StreamedResults>empty());
    final TestSubscriber<StreamedResults> subscriber = new TestSubscriber<>();
    subject.accept(NULL_PARAMS).subscribe(subscriber);
    subscriber.awaitTerminalEvent(2, TimeUnit.SECONDS);
    assertThat(subscriber.getLastSeenThread(), is(not(Thread.currentThread())));
  }
}

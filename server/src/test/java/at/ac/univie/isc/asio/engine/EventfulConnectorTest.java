package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.insight.Emitter;
import at.ac.univie.isc.asio.tool.EventMatchers;
import at.ac.univie.isc.asio.tool.Reactive;
import com.google.common.io.ByteStreams;
import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import rx.functions.Actions;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventfulConnectorTest {
  public static final Command NULL_PARAMS = CommandBuilder.empty().build();
  public static final StreamedResults DUMMY_RESULTS = new StreamedResults(MediaType.WILDCARD_TYPE) {
    @Override
    protected void doWrite(final OutputStream output) throws IOException {

    }
  };

  private final Emitter emitter = Mockito.mock(Emitter.class);
  private final Connector delegate = Mockito.mock(Connector.class);

  private final EventfulConnector subject = EventfulConnector.around(emitter, delegate);

  @Test
  public void wrapped_results_should_not_be_altered() throws Exception {
    when(delegate.accept(NULL_PARAMS)).thenReturn(Observable.just(DUMMY_RESULTS));
    final StreamedResults results = subject.accept(NULL_PARAMS).toBlocking().single();
    assertThat(results, sameInstance(DUMMY_RESULTS));
  }

  @Test
  public void should_emit_initial_received_event() throws Exception {
    when(delegate.accept(NULL_PARAMS)).thenReturn(Observable.<StreamedResults>empty());
    subject.accept(NULL_PARAMS);
    verify(emitter).emit(argThat(EventMatchers.hasSubject("received")));
  }

  @Test
  public void should_emit_executed_on_after_yielding_streamed_results() throws Exception {
    when(delegate.accept(NULL_PARAMS)).thenReturn(Observable.just(DUMMY_RESULTS));
    subject.accept(NULL_PARAMS).subscribe();
    verify(emitter).emit(argThat(EventMatchers.hasSubject("executed")));
  }

  @Test
  public void should_emit_failed_on_error() throws Exception {
    final IllegalStateException failure = new IllegalStateException("test");
    when(delegate.accept(NULL_PARAMS)).thenReturn(Observable.<StreamedResults>error(failure));
    subject.accept(NULL_PARAMS).subscribe(Actions.empty(), Reactive.ignoreErrors());
    verify(emitter).emit(argThat(EventMatchers.hasSubject("failed")));
  }

  @Test
  public void should_emit_completed_after_streaming_ended() throws Exception {
    when(delegate.accept(NULL_PARAMS)).thenReturn(Observable.just(DUMMY_RESULTS));
    subject.accept(NULL_PARAMS).toBlocking().single().write(ByteStreams.nullOutputStream());
    verify(emitter).emit(argThat(EventMatchers.hasSubject("completed")));
  }

  @Test
  public void should_emit_failed_when_stream_throws() throws Exception {
    final IllegalStateException failure = new IllegalStateException();
    final StreamedResults failing = new StreamedResults(MediaType.WILDCARD_TYPE) {
      @Override
      protected void doWrite(final OutputStream output) throws IOException {
        throw failure;
      }
    };
    when(delegate.accept(NULL_PARAMS)).thenReturn(Observable.just(failing));
    try {
      subject.accept(NULL_PARAMS).toBlocking().single().write(ByteStreams.nullOutputStream());
    } catch (Exception ignored) {}
    verify(emitter).emit(argThat(EventMatchers.hasSubject("failed")));
  }
}

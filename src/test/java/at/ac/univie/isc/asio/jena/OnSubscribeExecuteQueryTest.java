package at.ac.univie.isc.asio.jena;

import at.ac.univie.isc.asio.tool.Payload;
import at.ac.univie.isc.asio.tool.Reactive;
import at.ac.univie.isc.asio.tool.Unchecked;
import at.ac.univie.isc.asio.transport.ObservableStream;
import com.google.common.util.concurrent.Uninterruptibles;
import com.hp.hpl.jena.query.QueryExecution;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class OnSubscribeExecuteQueryTest {

  @Rule
  public Timeout timeout = new Timeout(2000);
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final QueryExecution query = mock(QueryExecution.class);
  private final JenaQueryHandler handler = mock(JenaQueryHandler.class);

  private Observable<ObservableStream> subject;
  private TestSubscriber<byte[]> subscriber;

  @Before
  public void setUp() throws Exception {
    subject = Observable
        .create(new OnSubscribeExecuteQuery(query, handler));
    subscriber = new TestSubscriber<>();
  }

  // ========================= SYNC BEHAVIOR

  @Test
  public void should_yield_serialized_query_results() throws Exception {
    final byte[] expected = Payload.randomWithLength(21_943);
    doAnswer(WriteToSink.use(expected)).when(handler).serialize(any(OutputStream.class));
    final byte[] result = subject
        .flatMap(Reactive.IDENTITY)
        .reduce(Reactive.BYTE_ACCUMULATOR)
        .toBlocking().single();
    assertThat(result, is(equalTo(expected)));
  }

  @Test
  public void should_close_query_after_results_consumed() throws Exception {
    doAnswer(WriteToSink.use(Payload.randomWithLength(2453)))
        .when(handler).serialize(any(OutputStream.class));
    subject.flatMap(Reactive.IDENTITY).toBlocking().last();
    verify(query).close();
  }

  @Test
  public void should_yield_execution_error() throws Exception {
    final RuntimeException cause = new RuntimeException("test");
    doThrow(cause).when(handler).invoke(any(QueryExecution.class));
    error.expect(is(cause));
    subject.flatMap(Reactive.IDENTITY).toBlocking().last();
  }

  @Test
  public void should_yield_serialization_error() throws Exception {
    final RuntimeException cause = new RuntimeException("test");
    doThrow(cause).when(handler).serialize(any(OutputStream.class));
    error.expect(is(cause));
    subject.flatMap(Reactive.IDENTITY).toBlocking().last();
  }

  @Test
  public void should_close_query_after_execution_error() throws Exception {
    doThrow(RuntimeException.class).when(handler).invoke(any(QueryExecution.class));
    subject.flatMap(Reactive.IDENTITY).subscribe(subscriber);
    subscriber.awaitTerminalEvent();
    verify(query).close();
  }

  @Test
  public void should_close_query_after_serialization_error() throws Exception {
    doThrow(RuntimeException.class).when(handler).serialize(any(OutputStream.class));
    subject.flatMap(Reactive.IDENTITY).subscribe(subscriber);
    subscriber.awaitTerminalEvent();
    verify(query).close();
  }

  // ========================= ASYNC SERIALIZATION

  @Test
  public void should_not_close_query_while_streaming() throws Exception {
    final CountDownLatch serializing = new CountDownLatch(1);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable {
        serializing.countDown(); // block observable
        Uninterruptibles.sleepUninterruptibly(4000, TimeUnit.MILLISECONDS);
        return null;
      }
    }).when(handler).serialize(any(OutputStream.class));
    final TestSubscriber<ObservableStream> querySubscriber = new TestSubscriber<>();
    final ConnectableObservable<ObservableStream> observableQuery = subject
        .subscribeOn(Schedulers.newThread())
        .publish();
    observableQuery.subscribe(querySubscriber);
    observableQuery.subscribe(new Action1<ObservableStream>() {
      @Override
      public void call(final ObservableStream stream) {
        stream.subscribeOn(Schedulers.newThread()).subscribe();
      }
    });
    observableQuery.connect();
    Unchecked.await(serializing);
    querySubscriber.awaitTerminalEvent();
    verify(query, never()).close();
  }

  @Test
  public void should_not_abort_query_after_observable_stream_has_been_yielded() throws Exception {
    final CountDownLatch streamReceived = new CountDownLatch(1);
    final Subscription subscription = subject
        .subscribeOn(Schedulers.newThread())
        .subscribe(new Action1<ObservableStream>() {
          @Override
          public void call(final ObservableStream observableStream) {
            streamReceived.countDown();
          }
        }
            , new Action1<Throwable>() {
          @Override
          public void call(final Throwable throwable) {
            return;
          }
        }
            , new Action0() {
          @Override
          public void call() {
            Uninterruptibles.sleepUninterruptibly(4000, TimeUnit.MILLISECONDS);
          }
        });
    streamReceived.await();
    subscription.unsubscribe();
    verify(query, never()).abort();
  }

  @Test
  public void should_abort_query_if_unsubscribing_before_execution_completed() throws Exception {
    final CountDownLatch invoked = new CountDownLatch(1);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable {
        invoked.countDown(); // block observable to allow concurrent unsubscribe
        Uninterruptibles.sleepUninterruptibly(4000, TimeUnit.MILLISECONDS);
        return null;
      }
    }).when(handler).invoke(any(QueryExecution.class));
    final Subscription subscription = subject
        .subscribeOn(Schedulers.newThread())
        .flatMap(Reactive.IDENTITY)
        .subscribe();
    Unchecked.await(invoked);
    subscription.unsubscribe();
    verify(query).abort();
  }

  @Test
  public void should_close_query_if_execution_is_aborted() throws Exception {
    final CountDownLatch executionStarted = new CountDownLatch(1);
    final CountDownLatch continueExecution = new CountDownLatch(1);
    final CountDownLatch closed = new CountDownLatch(1);
    doAnswer(new Answer() {
      @Override
      public Object answer(final InvocationOnMock invocation) throws Throwable {
        executionStarted.countDown();
        continueExecution.await();
        return null;
      }
    }).when(handler).invoke(any(QueryExecution.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(final InvocationOnMock invocation) throws Throwable {
        closed.countDown();
        return null;
      }
    }).when(query).close();
    final Subscription subscription = subject
        .subscribeOn(Schedulers.newThread())
        .subscribe();
    executionStarted.await();
    subscription.unsubscribe();
    continueExecution.countDown();
    closed.await();
    verify(query).close();
  }

  @Test
  public void should_abort_query_if_unsubscribing_during_serialization() throws Exception {
    final CountDownLatch serializing = new CountDownLatch(1);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable {
        serializing.countDown(); // block observable to allow concurrent unsubscribe
        Uninterruptibles.sleepUninterruptibly(4000, TimeUnit.MILLISECONDS);
        return null;
      }
    }).when(handler).serialize(any(OutputStream.class));
    final Subscription subscription = subject
        .toBlocking().single()
        .subscribeOn(Schedulers.newThread())
        .subscribe();
    serializing.await();
    subscription.unsubscribe();
    verify(query).abort();
  }

  @Test
  public void should_close_query_if_serialization_is_aborted() throws Exception {
    final CountDownLatch serializingStarted = new CountDownLatch(1);
    final CountDownLatch continueSerialization = new CountDownLatch(1);
    final CountDownLatch closed = new CountDownLatch(1);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable {
        serializingStarted.countDown(); // block observable to allow concurrent unsubscribe
        continueSerialization.await();
        return null;
      }
    }).when(handler).serialize(any(OutputStream.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(final InvocationOnMock invocation) throws Throwable {
        closed.countDown();
        return null;
      }
    }).when(query).close();
    final Subscription subscription = subject
        .toBlocking().single()
        .subscribeOn(Schedulers.newThread())
        .subscribe();
    serializingStarted.await();
    subscription.unsubscribe();
    continueSerialization.countDown();
    closed.await();
    verify(query).close();
  }

  private static final class WriteToSink implements Answer<Void> {
    public static WriteToSink use(final byte[] data) {
      return new WriteToSink(data);
    }

    private WriteToSink(final byte[] data) {
      payload = data;
    }

    private final byte[] payload;

    @Override
    public Void answer(final InvocationOnMock invocation) throws Throwable {
      final OutputStream sink = (OutputStream) invocation.getArguments()[0];
      sink.write(payload);
      return null;
    }
  }
}

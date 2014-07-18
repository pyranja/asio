package at.ac.univie.isc.asio.jena;

import at.ac.univie.isc.asio.Command;
import at.ac.univie.isc.asio.tool.Payload;
import at.ac.univie.isc.asio.tool.Resources;
import at.ac.univie.isc.asio.tool.Rules;
import at.ac.univie.isc.asio.tool.Unchecked;
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
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class OnSubscribeExecuteQueryTest {

  @Rule
  public Timeout timeout = Rules.timeout(2, TimeUnit.SECONDS);
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final QueryExecution query = mock(QueryExecution.class);
  private final JenaQueryHandler handler = mock(JenaQueryHandler.class);

  private Observable<Command.Results> subject;
  private TestSubscriber<Command.Results> subscriber;
  private ByteArrayOutputStream sink;

  @Before
  public void setUp() throws Exception {
    subject = Observable.create(new OnSubscribeExecuteQuery(query, handler));
    final Observer<Command.Results> consumer = new Observer<Command.Results>() {
      @Override
      public void onCompleted() {
      }

      @Override
      public void onError(final Throwable e) {
      }

      @Override
      public void onNext(final Command.Results results) {
        Unchecked.write(results, sink);
      }
    };
    subscriber = new TestSubscriber<>(consumer);
    sink = new ByteArrayOutputStream();
  }

  // ========================= SYNC BEHAVIOR

  @Test
  public void should_yield_serialized_query_results() throws Exception {
    final byte[] expected = Payload.randomWithLength(21_943);
    doAnswer(WriteToSink.use(expected)).when(handler).serialize(any(OutputStream.class));
    subject.toBlocking().single().write(sink);
    assertThat(sink.toByteArray(), is(equalTo(expected)));
  }

  @Test
  public void should_close_query_after_results_consumed() throws Exception {
    doAnswer(WriteToSink.use(Payload.randomWithLength(2453)))
        .when(handler).serialize(any(OutputStream.class));
    subject.toBlocking().single().write(sink);
    verify(query).close();
  }

  @Test
  public void should_yield_execution_error() throws Exception {
    final RuntimeException cause = new RuntimeException("test");
    doThrow(cause).when(handler).invoke(any(QueryExecution.class));
    error.expect(is(cause));
    subject.toBlocking().last();
  }

  @Test
  public void should_yield_serialization_error() throws Exception {
    final RuntimeException cause = new RuntimeException("test");
    doThrow(cause).when(handler).serialize(any(OutputStream.class));
    error.expect(is(cause));
    subject.toBlocking().single().write(sink);
  }

  @Test
  public void should_close_query_after_execution_error() throws Exception {
    doThrow(RuntimeException.class).when(handler).invoke(any(QueryExecution.class));
    subject.subscribe(subscriber);
    subscriber.awaitTerminalEvent();
    verify(query).close();
  }

  @Test
  public void should_close_query_after_serialization_error() throws Exception {
    doThrow(RuntimeException.class).when(handler).serialize(any(OutputStream.class));
    subject.subscribe(subscriber);
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
    final TestSubscriber<StreamingOutput> querySubscriber = new TestSubscriber<>();
    final ConnectableObservable<Command.Results> observableQuery = subject
        .subscribeOn(Schedulers.newThread())
        .publish();
    observableQuery.subscribe(querySubscriber);
    observableQuery.subscribe(new Action1<StreamingOutput>() {
      @Override
      public void call(final StreamingOutput streamingOutput) {
        Unchecked.write(streamingOutput, sink);
      }
    });
    observableQuery.connect();
    Unchecked.await(serializing);
    verify(query, never()).close();
  }

  @Test
  public void should_not_allow_cancelling_after_stream_has_been_yielded() throws Exception {
    final CountDownLatch streamReceived = new CountDownLatch(1);
    final Subscription subscription = subject
        .subscribeOn(Schedulers.newThread())
        .subscribe(new Action1<StreamingOutput>() {
          @Override
          public void call(final StreamingOutput streamingOutput) {
            streamReceived.countDown();
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
  public void should_abort_query_if_closing_results_before_consumption() throws Exception {
    final CountDownLatch closed = new CountDownLatch(1);
    final CountDownLatch serializing = new CountDownLatch(1);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable {
        serializing.countDown(); // block observable to allow concurrent unsubscribe
        Uninterruptibles.sleepUninterruptibly(4000, TimeUnit.MILLISECONDS);
        return null;
      }
    }).when(handler).serialize(any(OutputStream.class));
    subject.subscribe(new Action1<Command.Results>() {
      @Override
      public void call(final Command.Results results) {
        Schedulers.newThread().createWorker().schedule(new Action0() {
          @Override
          public void call() {
            Unchecked.write(results, sink);
          }
        });
        Unchecked.await(serializing);
        Resources.close(results);
        closed.countDown();
      }
    });
    closed.await();
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
    subject
        .subscribe(new Action1<Command.Results>() {
          @Override
          public void call(final Command.Results results) {
            Schedulers.newThread().createWorker().schedule(new Action0() {
              @Override
              public void call() {
                Unchecked.write(results, sink);
              }
            });
            Unchecked.await(serializingStarted);
            Resources.close(results);
            continueSerialization.countDown();
          }
        });
    serializingStarted.await();
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

package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.tool.Resources;
import at.ac.univie.isc.asio.junit.Rules;
import at.ac.univie.isc.asio.Unchecked;
import com.google.common.util.concurrent.Uninterruptibles;
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
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class OnSubscribeExecuteTest {
  @Rule
  public Timeout timeout = Rules.timeout(2, TimeUnit.SECONDS);
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final Invocation delegate = mock(Invocation.class);

  private Observable<StreamedResults> subject;
  private TestSubscriber<StreamedResults> subscriber;
  private ByteArrayOutputStream sink;

  @Before
  public void setUp() throws Exception {
    subject = Observable.create(OnSubscribeExecute.given(delegate));
    final Observer<StreamedResults> consumer = new Observer<StreamedResults>() {
      @Override
      public void onCompleted() {
      }

      @Override
      public void onError(final Throwable e) {
      }

      @Override
      public void onNext(final StreamedResults results) {
        Unchecked.write(results, sink);
      }
    };
    subscriber = new TestSubscriber<StreamedResults>(consumer);
    sink = new ByteArrayOutputStream();
  }

  // ========================= SYNC BEHAVIOR

  @Test
  public void should_yield_operation_results() throws Exception {
    final byte[] expected = Payload.randomWithLength(21_943);
    doAnswer(WriteToSink.use(expected)).when(delegate).write(any(OutputStream.class));
    subject.toBlocking().single().write(sink);
    assertThat(sink.toByteArray(), is(equalTo(expected)));
  }

  @Test
  public void should_close_operation_after_results_consumed() throws Exception {
    doAnswer(WriteToSink.use(Payload.randomWithLength(2453)))
        .when(delegate).write(any(OutputStream.class));
    subject.toBlocking().single().write(sink);
    verify(delegate).close();
  }

  @Test
  public void should_yield_execution_error() throws Exception {
    final RuntimeException cause = new RuntimeException("test");
    doThrow(cause).when(delegate).execute();
    error.expect(is(cause));
    subject.toBlocking().last();
  }

  @Test
  public void should_yield_serialization_error() throws Exception {
    final RuntimeException cause = new RuntimeException("test");
    doThrow(cause).when(delegate).write(any(OutputStream.class));
    error.expect(is(cause));
    subject.toBlocking().single().write(sink);
  }

  @Test
  public void should_close_operation_after_execution_error() throws Exception {
    doThrow(RuntimeException.class).when(delegate).execute();
    subject.subscribe(subscriber);
    subscriber.awaitTerminalEvent();
    verify(delegate).close();
  }

  @Test
  public void should_close_operation_after_serialization_error() throws Exception {
    doThrow(RuntimeException.class).when(delegate).write(any(OutputStream.class));
    subject.subscribe(subscriber);
    subscriber.awaitTerminalEvent();
    verify(delegate).close();
  }

  // ========================= ASYNC SERIALIZATION

  @Test
  public void should_not_close_operation_while_streaming() throws Exception {
    final CountDownLatch serializing = new CountDownLatch(1);
    final AtomicBoolean completed = new AtomicBoolean(false);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable {
        serializing.countDown(); // block observable
        Uninterruptibles.sleepUninterruptibly(4000, TimeUnit.MILLISECONDS);
        return null;
      }
    }).when(delegate).write(any(OutputStream.class));
    subject
        .subscribeOn(Schedulers.newThread())
        .subscribe(new Action1<StreamingOutput>() {
          @Override
          public void call(final StreamingOutput streamingOutput) {
            Unchecked.write(streamingOutput, sink);
          }
        });
    Unchecked.await(serializing);
    verify(delegate, never()).close();
  }

  @Test
  public void should_not_allow_cancelling_after_stream_has_been_yielded() throws Exception {
    final CountDownLatch streamReceived = new CountDownLatch(1);
    final Subscription subscription = subject
        .subscribeOn(Schedulers.newThread())
        .subscribe(new Action1<StreamingOutput>() {
          @Override
          public void call(final StreamingOutput streamingOutput) {
            Unchecked.write(streamingOutput, sink);
            streamReceived.countDown();
          }
        });
    streamReceived.await();
    subscription.unsubscribe();
    verify(delegate, never()).cancel();
  }

  @Test
  public void should_abort_operation_if_unsubscribing_before_execution_completed() throws Exception {
    final CountDownLatch executed = new CountDownLatch(1);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable {
        executed.countDown(); // block observable to allow concurrent unsubscribe
        Uninterruptibles.sleepUninterruptibly(4000, TimeUnit.MILLISECONDS);
        return null;
      }
    }).when(delegate).execute();
    final Subscription subscription = subject
        .subscribeOn(Schedulers.newThread())
        .subscribe();
    Unchecked.await(executed);
    subscription.unsubscribe();
    verify(delegate).cancel();
  }

  @Test
  public void should_close_operation_if_execution_is_aborted() throws Exception {
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
    }).when(delegate).execute();
    doAnswer(new Answer() {
      @Override
      public Object answer(final InvocationOnMock invocation) throws Throwable {
        closed.countDown();
        return null;
      }
    }).when(delegate).close();
    final Subscription subscription = subject
        .subscribeOn(Schedulers.newThread())
        .subscribe();
    executionStarted.await();
    subscription.unsubscribe();
    continueExecution.countDown();
    closed.await();
    verify(delegate).close();
  }

  @Test
  public void should_abort_operation_if_closing_results_before_consumption() throws Exception {
    final CountDownLatch closed = new CountDownLatch(1);
    final CountDownLatch serializing = new CountDownLatch(1);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable {
        serializing.countDown(); // block observable to allow concurrent unsubscribe
        Uninterruptibles.sleepUninterruptibly(4000, TimeUnit.MILLISECONDS);
        return null;
      }
    }).when(delegate).write(any(OutputStream.class));
    subject.subscribe(new Action1<StreamedResults>() {
      @Override
      public void call(final StreamedResults results) {
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
    verify(delegate).cancel();
  }

  @Test
  public void should_close_operation_if_serialization_is_aborted() throws Exception {
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
    }).when(delegate).write(any(OutputStream.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(final InvocationOnMock invocation) throws Throwable {
        closed.countDown();
        return null;
      }
    }).when(delegate).close();
    subject
        .subscribe(new Action1<StreamedResults>() {
          @Override
          public void call(final StreamedResults results) {
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
    verify(delegate).close();
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

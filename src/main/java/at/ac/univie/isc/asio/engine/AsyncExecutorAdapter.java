package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetTransportException;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.transport.ObservableStream;
import com.google.common.io.InputSupplier;
import com.google.common.util.concurrent.ListenableFuture;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

import static at.ac.univie.isc.asio.tool.Reactive.listeningFor;
import static java.util.Objects.requireNonNull;

public class AsyncExecutorAdapter implements Engine {

  private final AsyncExecutor adapted;
  private final Scheduler scheduler;

  public AsyncExecutorAdapter(
      @Nonnull final AsyncExecutor adapted, @Nonnull ExecutorService exec) {
    this.adapted = requireNonNull(adapted);
    scheduler = Schedulers.from(requireNonNull(exec));
  }

  @Override
  public Observable<ObservableStream> execute(@Nonnull final DatasetOperation operation) {
    requireNonNull(operation);
    try {
      final ListenableFuture<Result> future = adapted.accept(operation);
      return Observable.create(listeningFor(future))
          .map(new Func1<Result, ObservableStream>() {
        @Override
        public ObservableStream call(final Result result) {
          return ObservableStream.from(unchecked(result));
        }
      }).observeOn(scheduler);
    } catch (Exception e) {
      return Observable.error(e);
    }
  }

  private static InputStream unchecked(InputSupplier<InputStream> supplier) {
    try {
      return supplier.getInput();
    } catch (IOException e) {
      throw new DatasetTransportException(e);
    }
  }

}

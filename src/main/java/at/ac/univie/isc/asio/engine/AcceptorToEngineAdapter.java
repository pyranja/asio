package at.ac.univie.isc.asio.engine;

import com.google.common.io.InputSupplier;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetTransportException;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.transport.ObservableStream;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static at.ac.univie.isc.asio.common.ToObservableListenableFuture.listeningFor;
import static java.util.Objects.requireNonNull;

/**
 * @author pyranja
 */
public class AcceptorToEngineAdapter implements Engine {

  private final OperationAcceptor adapted;
  private final Scheduler scheduler;

  public AcceptorToEngineAdapter(
      @Nonnull final OperationAcceptor adapted, @Nonnull ExecutorService exec) {
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

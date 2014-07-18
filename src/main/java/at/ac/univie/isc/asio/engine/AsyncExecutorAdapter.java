package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Command;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.Result;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ListenableFuture;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import javax.annotation.Nonnull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

import static at.ac.univie.isc.asio.tool.Reactive.listeningFor;
import static java.util.Objects.requireNonNull;

public class AsyncExecutorAdapter implements ReactiveOperationExecutor {

  private final AsyncExecutor adapted;
  private final Scheduler scheduler;

  public AsyncExecutorAdapter(
      @Nonnull final AsyncExecutor adapted, @Nonnull ExecutorService exec) {
    this.adapted = requireNonNull(adapted);
    scheduler = Schedulers.from(requireNonNull(exec));
  }

  @Override
  public Observable<Command.Results> execute(@Nonnull final DatasetOperation operation) {
    requireNonNull(operation);
    try {
      final ListenableFuture<Result> future = adapted.accept(operation);
      return Observable.create(listeningFor(future))
          .map(new Func1<Result, Command.Results>() {
            @Override
            public Command.Results call(final Result result) {
              return new Command.Results() {
                @Override
                public void write(final OutputStream output) throws IOException, WebApplicationException {
                  try (final InputStream source = result.getInput()) {
                    ByteStreams.copy(source, output);
                  }
                }

                @Override
                public MediaType format() {
                  return MediaType.valueOf(result.mediaType().toString());
                }

                @Override
                public void close() {
                  try {
                    result.getInput().close();
                  } catch (IOException e) {
                    Throwables.propagate(e);
                  }
                }
              };
            }
          })
          .observeOn(scheduler);
    } catch (Exception e) {
      return Observable.error(e);
    }
  }
}

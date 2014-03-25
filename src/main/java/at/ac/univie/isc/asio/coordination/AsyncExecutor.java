package at.ac.univie.isc.asio.coordination;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.transport.Transfer;

import com.google.common.base.Supplier;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class AsyncExecutor implements OperationAcceptor {

  private final Supplier<Transfer> transferFactory;
  private final Operator delegate;

  public AsyncExecutor(final Supplier<Transfer> transferFactory, final Operator delegate) {
    super();
    this.transferFactory = transferFactory;
    this.delegate = delegate;
  }

  @Override
  public ListenableFuture<Result> accept(final DatasetOperation operation) {
    try {
      final Transfer transfer = transferFactory.get();
      final ExecutionChain callback = new ExecutionChain(operation, transfer);
      delegate.invoke(operation, transfer, callback);
      return callback.future();
    } catch (final DatasetException e) {
      e.setFailedOperation(operation);
      return Futures.immediateFailedFuture(e);
    } catch (final Exception e) {
      final DatasetFailureException wrapper = new DatasetFailureException(e);
      wrapper.setFailedOperation(operation);
      return Futures.immediateFailedFuture(wrapper);
    }
  }

  private static class ExecutionChain implements OperatorCallback {

    /* slf4j-logger */
    private final static Logger log = LoggerFactory.getLogger(ExecutionChain.class);

    private final DatasetOperation operation;
    private final Transfer exchange;
    private final SettableFuture<Result> promise;
    private final Set<Phase> steps;

    ExecutionChain(final DatasetOperation operation, final Transfer exchange) {
      super();
      this.operation = operation;
      this.exchange = exchange;
      steps =
          Collections.synchronizedSet(EnumSet.of(Phase.DELIVERY, Phase.EXECUTION,
              Phase.SERIALIZATION));
      promise = SettableFuture.create();
    }

    @Override
    public void completed(final Phase completed) {
      log.trace("received {} for {}", completed, operation);
      assert steps.contains(completed) : "phase " + completed + " completed twice";
      steps.remove(completed);
      if (Phase.EXECUTION == completed) {
        promise.set(new Result() {
          @Override
          public InputStream getInput() throws IOException {
            return Channels.newInputStream(exchange.source());
          }

          @Override
          public MediaType mediaType() {
            return operation.format().asMediaType();
          }
        });
      }
      if (steps.isEmpty()) {
        log.trace("completed execution of {}", operation);
        exchange.release();
      }
    }

    @Override
    public void fail(final DatasetException cause) {
      log.warn("received {} for {}", cause, operation);
      promise.setException(cause);
    }

    public ListenableFuture<Result> future() {
      return promise;
    }
  }
}

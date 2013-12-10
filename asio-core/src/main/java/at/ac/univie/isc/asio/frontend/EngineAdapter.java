package at.ac.univie.isc.asio.frontend;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.ws.rs.core.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.coordination.Operator;
import at.ac.univie.isc.asio.coordination.OperatorCallback;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;
import at.ac.univie.isc.asio.transport.JdkPipeTransfer;
import at.ac.univie.isc.asio.transport.Transfer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Enhances a {@link DatasetEngine} with content negotiation functionality and handles result
 * management.
 * 
 * @author Chris Borckholder
 */
public class EngineAdapter {

  /**
   * convenience factory method for testing
   */
  @VisibleForTesting
  // FIXME remove this hack !
  static EngineAdapter adapt(final DatasetEngine engine) {
    final FormatSelector selector = new FormatSelector(engine.supports(), new VariantConverter());
    return new EngineAdapter(engine, selector);
  }

  private final DatasetEngine delegate;
  private final FormatSelector selector;

  public EngineAdapter(final DatasetEngine delegate, final FormatSelector selector) {
    super();
    this.delegate = delegate;
    this.selector = selector;
  }

  /**
   * Attempt to complete the given {@link OperationBuilder partial operation} with a
   * {@link SerializationFormat} that is supported by the backing engine and is compatible to the
   * content types accepted by the given request.
   * 
   * @param request holding acceptable variants
   * @param partial operation in construction
   * @return completed operation with the selected format
   * @throws ActionNotSupportedException if the given action is not supported by the backing engine
   * @throws VariantsNotAcceptableException if none of the accepted content types in the request can
   *         be mapped to a {@link SerializationFormat}
   */
  public DatasetOperation completeWithMatchingFormat(final Request request,
      final OperationBuilder partial) {
    try {
      final SerializationFormat selected = selector.selectFormat(request, partial.getAction());
      return partial.renderAs(selected);
    } catch (final DatasetUsageException e) {
      // set contextual information on error
      e.setFailedOperation(partial.invalidate());
      throw e;
    }
  }

  /**
   * Prepare result storage and forward the given operation to the enclosed {@link DatasetEngine}.
   * If an error occurs, wrap it as a {@link DatasetException} if necessary and return a failed
   * future.
   * 
   * @param operation to be executed
   * @return future holding the result or an exception if the submission fails
   */
  public ListenableFuture<Result> submit(final DatasetOperation operation) {
    try {
      final Transfer transfer = new JdkPipeTransfer(); // XXX inject factory
      final Operator operator = delegate;
      final Execution exec = new Execution(operation, transfer);
      operator.invoke(operation, transfer, exec);
      return exec.asFuture();
    } catch (final DatasetException e) {
      e.setFailedOperation(operation);
      return Futures.immediateFailedFuture(e);
    } catch (final Exception e) {
      final DatasetFailureException wrapper = new DatasetFailureException(e);
      wrapper.setFailedOperation(operation);
      return Futures.immediateFailedFuture(wrapper);
    }
  }

  private static class Execution implements OperatorCallback {

    /* slf4j-logger */
    private final static Logger log = LoggerFactory.getLogger(EngineAdapter.Execution.class);

    private final DatasetOperation operation;
    private final Transfer exchange;
    private final SettableFuture<Result> adaptee;
    private final Set<Phase> steps;

    Execution(final DatasetOperation operation, final Transfer exchange) {
      super();
      this.operation = operation;
      this.exchange = exchange;
      steps =
          Collections.synchronizedSet(EnumSet.of(Phase.DELIVERY, Phase.EXECUTION,
              Phase.SERIALIZATION));
      adaptee = SettableFuture.create();
    }

    @Override
    public void completed(final Phase completed) {
      log.trace("received {} for {}", completed, operation);
      assert steps.contains(completed) : "phase " + completed + " completed twice";
      steps.remove(completed);
      if (Phase.EXECUTION == completed) {
        adaptee.set(new Result() {
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
      adaptee.setException(cause);
    }

    public ListenableFuture<Result> asFuture() {
      return adaptee;
    }
  }
}

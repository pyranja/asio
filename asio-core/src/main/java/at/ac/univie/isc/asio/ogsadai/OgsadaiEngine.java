package at.ac.univie.isc.asio.ogsadai;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.ogsadai.activity.event.CompletionCallback;
import uk.org.ogsadai.activity.workflow.Workflow;
import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.coordination.EngineSpec;
import at.ac.univie.isc.asio.coordination.Operator;
import at.ac.univie.isc.asio.coordination.OperatorCallback;
import at.ac.univie.isc.asio.coordination.OperatorCallback.Phase;
import at.ac.univie.isc.asio.transport.Transfer;

import com.google.common.io.OutputSupplier;

/**
 * Defer DatasetOperations to an OGSADAI server instance.
 * 
 * @author Chris Borckholder
 */
public final class OgsadaiEngine implements DatasetEngine, Operator {

  /* slf4j-logger */
  final static Logger log = LoggerFactory.getLogger(OgsadaiEngine.class);

  private final OgsadaiAdapter ogsadai;
  private final WorkflowComposer composer;
  private final DaiExceptionTranslator translator;

  OgsadaiEngine(final OgsadaiAdapter ogsadai, final WorkflowComposer composer,
      final DaiExceptionTranslator translator) {
    super();
    this.ogsadai = ogsadai;
    this.composer = composer;
    this.translator = translator;
  }

  /**
   * @return all {@link OgsadaiFormats formats} supported by OGSADAI.
   */
  @Override
  public Set<SerializationFormat> supports() {
    return OgsadaiFormats.asSet();
  }

  @Override
  public Type type() {
    return EngineSpec.Type.SQL;
  }

  @Override
  public void invoke(final DatasetOperation operation, final Transfer exchange,
      final OperatorCallback handler) {
    final Workflow workflow =
        composer.createFrom(operation, wrapAsSupplier(exchange.sink(), handler));
    log.trace("-- using workflow :\n{}", workflow);
    final CompletionCallback callback = delegateTo(handler, operation);
    log.debug(">> invoking OGSADAI request");
    ogsadai.invoke(operation.id(), workflow, callback);
    log.debug("<< OGSADAI request invoked");
  }

  private CompletionCallback delegateTo(final OperatorCallback callback,
      final DatasetOperation operation) {
    return new CompletionCallback() {
      @Override
      public void complete() {
        callback.completed(Phase.SERIALIZATION);
      }

      @Override
      public void fail(final Exception cause) {
        final DatasetException error = translator.translate(cause);
        error.setFailedOperation(operation);
        callback.fail(error);
      }
    };
  }

  private OutputSupplier<OutputStream> wrapAsSupplier(final WritableByteChannel channel,
      final OperatorCallback callback) {
    return new OutputSupplier<OutputStream>() {
      @Override
      public OutputStream getOutput() throws IOException {
        callback.completed(Phase.EXECUTION);
        return Channels.newOutputStream(channel);
      }
    };
  }
}

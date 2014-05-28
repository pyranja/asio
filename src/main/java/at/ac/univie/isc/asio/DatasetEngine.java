package at.ac.univie.isc.asio;

import java.util.Set;

import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.engine.EngineSpec;
import at.ac.univie.isc.asio.engine.Operator;
import at.ac.univie.isc.asio.engine.OperatorCallback;
import at.ac.univie.isc.asio.transport.Transfer;

/**
 * Accept and execute DatasetOperations. Execution may be asynchronous.
 * 
 * @author Chris Borckholder
 */
public interface DatasetEngine extends Operator, EngineSpec {

  /**
   * Execute the given {@link DatasetOperation operation}. The results MUST be delivered through the
   * given {@link Transfer exchange} in a format that is compatible to the
   * {@link SerializationFormat one} given in the {@link DatasetOperation#format() operation}.
   * <p>
   * Implementations MUST report progress to the supplied {@link OperatorCallback callback} in the
   * correct order, i.e. first {@link OperatorCallback.Phase#EXECUTION} after successfully
   * performing the given command, then {@link OperatorCallback.Phase#SERIALIZATION} after
   * transformed results were written to the given {@link Transfer exchange}.
   * </p>
   * <p>
   * Implementations SHOULD NOT throw exceptions if the operation is unacceptable or an error occurs
   * during execution, but set the appropriate error state on the
   * {@link OperatorCallback#fail(DatasetException) callback}.
   * </p>
   * 
   * @param operation to be executed
   * @param exchange an unidirectional pipe for the results
   * @param callback to the execution engine for progress updates and/or error reporting
   */
  @Override
  void invoke(DatasetOperation operation, Transfer exchange, OperatorCallback callback);

  /**
   * @return all {@link SerializationFormat result formats} supported by this engine.
   */
  @Override
  Set<SerializationFormat> supports();
}

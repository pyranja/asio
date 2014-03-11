package at.ac.univie.isc.asio.coordination;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.Result;

import com.google.common.util.concurrent.ListenableFuture;

public interface OperationAcceptor {

  /**
   * Execute the given operation asynchronously.
   * 
   * @param operation to be executed
   * @return future result
   */
  ListenableFuture<Result> accept(DatasetOperation operation);
}

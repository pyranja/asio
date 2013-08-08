package at.ac.univie.isc.asio;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Store dataset operation results.
 * 
 * @author Chris Borckholder
 */
public interface ResultRepository {

  /**
   * Prepare storage for the results of given operation.
   * 
   * @param operation that generates the result
   * @return prepared handler
   * @throws DatasetTransportException on IO error
   */
  ResultHandler newHandlerFor(DatasetOperation operation) throws DatasetTransportException;

  /**
   * Retrieve the (possible pending) results for the DatsetOperation with given id.
   * 
   * @param opId of operation
   * @return future holding results.
   * @throws DatasetTransportException on IO error
   * @throws DatasetUsageException if no operation with given id is known
   */
  ListenableFuture<Result> find(String opId) throws DatasetTransportException,
      DatasetUsageException;

  /**
   * Delete any results for the operation with given id.
   * 
   * @param opId of operation
   * @return true if the operation existed, false else.
   */
  boolean delete(String opId);
}

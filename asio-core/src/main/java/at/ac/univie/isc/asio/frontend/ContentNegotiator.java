package at.ac.univie.isc.asio.frontend;

import javax.ws.rs.core.Request;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.DatasetUsageException;

public interface ContentNegotiator {

  /**
   * Determine the appropriate {@link SerializationFormat response format} for the given request and
   * action.
   * 
   * @param request JAX-RS HTTP request
   * @param action to be executed
   * @return the selected format
   * @throws DatasetUsageException if either the given action is not supported or no acceptable
   *         format is found
   */
  SerializationFormat negotiate(Request request, Action action);
}

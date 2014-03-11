package at.ac.univie.isc.asio.frontend;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Request;

import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;

/**
 * Complete format negotiation and route the operation to the correct engine.
 * 
 * @author Chris Borckholder
 */
// TODO merge with engine adapter / registry / etc. ...
public interface OperationRouter {

  /**
   * Complete the operation information and execute it.
   * 
   * @param partial dataset operation
   * @param request JAXRS request
   * @param response JAXRS suspended response
   */
  public void accept(final OperationBuilder partial, final Language language,
      final Request request, final AsyncResponse response);
}

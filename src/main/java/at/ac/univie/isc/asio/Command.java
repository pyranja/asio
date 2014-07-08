package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.transport.ObservableStream;
import rx.Observable;

import javax.ws.rs.core.MediaType;

/**
 *
 */
public interface Command {

  MediaType format();

  Role requiredRole();

  Observable<ObservableStream> observe();
}

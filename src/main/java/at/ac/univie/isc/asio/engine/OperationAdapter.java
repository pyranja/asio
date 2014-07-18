package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Command;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.security.Role;
import rx.Observable;

import javax.ws.rs.core.MediaType;

public class OperationAdapter implements Command {

  private final MediaType format;
  // delegates
  private final DatasetOperation inner;
  private final ReactiveOperationExecutor backend;

  public OperationAdapter(final MediaType format, final DatasetOperation inner, final ReactiveOperationExecutor backend) {
    this.format = format;
    this.inner = inner;
    this.backend = backend;
  }

  @Override
  public MediaType format() {
    return format;
  }

  @Override
  public Role requiredRole() {
    switch (inner.action()) {
      case SCHEMA:
      case QUERY:
        return Role.READ;
      case UPDATE:
        return Role.WRITE;
      default:
        throw new AssertionError("illegal action " + inner.action());
    }
  }

  @Override
  public Observable<Results> observe() {
    return backend.execute(inner);
  }
}

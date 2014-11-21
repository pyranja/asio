package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetException;
import rx.functions.Action0;
import rx.functions.Action1;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;

class ResultsProxy implements Command.Results {
  private final Command.Results delegate;
  private Action0 successHandler = new Action0() {
    @Override
    public void call() {
      // noop
    }
  };
  private Action1<Throwable> errorHandler = new Action1<Throwable>() {

    @Override
    public void call(final Throwable throwable) {
      // noop
    }
  };

  public static ResultsProxy wrap(final Command.Results delegate) {
    return new ResultsProxy(delegate);
  }

  private ResultsProxy(final Command.Results delegate) {
    this.delegate = delegate;
  }

  public ResultsProxy onSuccess(final Action0 handler) {
    successHandler = handler;
    return this;
  }

  public ResultsProxy onError(final Action1<Throwable> handler) {
    errorHandler = handler;
    return this;
  }

  @Override
  public void write(final OutputStream output) throws IOException, DatasetException {
    try {
      delegate.write(output);
      successHandler.call();
    } catch (final Throwable t) {
      errorHandler.call(t);
      throw t;
    }
  }

  @Override
  public MediaType format() {
    return delegate.format();
  }

  @Override
  public void close() throws DatasetException {
    delegate.close();
  }
}

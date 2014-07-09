package at.ac.univie.isc.asio.jena;

import at.ac.univie.isc.asio.engine.OperatorCallback;
import at.ac.univie.isc.asio.engine.OperatorCallback.Phase;
import at.ac.univie.isc.asio.transport.Transfer;
import com.hp.hpl.jena.query.QueryExecution;

import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.concurrent.Callable;

class SparqlRunner<RESULT> implements Callable<Void> {

  public static <RESULT> SparqlRunner<RESULT> create(final Transfer exchange,
      final OperatorCallback callback, final QueryModeHandler<RESULT> handler) {
    return new SparqlRunner<>(exchange, callback, handler);
  }

  private final Transfer exchange;
  private final OperatorCallback callback;
  private final QueryModeHandler<RESULT> handler;

  private QueryExecution execution;

  public SparqlRunner(final Transfer exchange, final OperatorCallback callback,
      final QueryModeHandler<RESULT> handler) {
    super();
    this.exchange = exchange;
    this.callback = callback;
    this.handler = handler;
  }

  public SparqlRunner<RESULT> use(final QueryExecution execution) {
    this.execution = execution;
    return this;
  }

  @Override
  public Void call() throws Exception {
    assert execution != null : "sparql runner not initalized with query execution";
    try (CloseableQueryExecution proxy = new CloseableQueryExecution(execution)) {
      handler.invoke(proxy.get());
      callback.completed(Phase.EXECUTION);
      try (OutputStream sink = Channels.newOutputStream(exchange.sink())) {
        handler.serialize(sink);
      }
      callback.completed(Phase.SERIALIZATION);
    }
    return null;
  }
}

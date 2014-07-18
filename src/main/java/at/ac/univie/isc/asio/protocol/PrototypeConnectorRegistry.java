package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.Connector;
import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.engine.AsyncExecutorAdapter;
import at.ac.univie.isc.asio.engine.AsyncExecutor;
import at.ac.univie.isc.asio.engine.ReactiveOperationExecutor;
import at.ac.univie.isc.asio.engine.EngineSpec.Type;
import at.ac.univie.isc.asio.engine.OperationFactory;
import at.ac.univie.isc.asio.transport.Transfer;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class PrototypeConnectorRegistry implements Registry {

  private final Map<Type, DatasetEngine> available;
  private final Supplier<Transfer> transferFactory;
  private final ExecutorService responseThreadPool;
  private final OperationFactory create;

  public PrototypeConnectorRegistry(final Set<DatasetEngine> engines,
                                    final Supplier<Transfer> transferFactory,
                                    final ExecutorService responseThreadPool, final OperationFactory create) {
    super();
    this.transferFactory = transferFactory;
    this.responseThreadPool = responseThreadPool;
    this.create = create;
    final Builder<Type, DatasetEngine> product = ImmutableMap.builder();
    for (final DatasetEngine each : engines) {
      product.put(each.type(), each);
    }
    available = product.build();
  }

  private DatasetEngine engineFor(final Language language) {
    final DatasetEngine engine = available.get(language.asEngineType());
    if (engine == null) {
      final String message =
          String.format(Locale.ENGLISH, "unsupported query language : %s", language);
      throw new DatasetUsageException(message);
    }
    return engine;
  }

  @Override
  public Connector find(final Language language) {
    final DatasetEngine engine = engineFor(language);
    final ReactiveOperationExecutor acceptor = new AsyncExecutorAdapter(new AsyncExecutor(transferFactory, engine), responseThreadPool);
    final FormatMatcher matcher = new FormatMatcher(engine.supports());
    return new CommandFactory(matcher, acceptor, create);
  }
}

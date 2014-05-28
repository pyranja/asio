package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.config.TimeoutSpec;
import at.ac.univie.isc.asio.engine.AcceptorToEngineAdapter;
import at.ac.univie.isc.asio.engine.AsyncExecutor;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.EngineSpec.Type;
import at.ac.univie.isc.asio.frontend.AsyncProcessor;
import at.ac.univie.isc.asio.frontend.FormatSelector;
import at.ac.univie.isc.asio.frontend.VariantConverter;
import at.ac.univie.isc.asio.transport.Transfer;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class PrototypeEngineProvider implements EndpointSupplier {

  private final OperationParser parser;
  private final AsyncProcessor processor;
  private final Map<Type, DatasetEngine> available;
  private final Supplier<Transfer> transferFactory;
  private final ExecutorService responseThreadPool;
  private final TimeoutSpec timeout;

  public PrototypeEngineProvider(final Set<DatasetEngine> engines, final OperationParser parser,
                                 final AsyncProcessor processor, final Supplier<Transfer> transferFactory, final ExecutorService responseThreadPool, final TimeoutSpec timeout) {
    super();
    this.parser = parser;
    this.processor = processor;
    this.transferFactory = transferFactory;
    this.responseThreadPool = responseThreadPool;
    this.timeout = timeout;
    final Builder<Type, DatasetEngine> product = ImmutableMap.builder();
    for (final DatasetEngine each : engines) {
      product.put(each.type(), each);
    }
    available = product.build();
  }

  /* (non-Javadoc)
   * @see at.ac.univie.isc.asio.protocol.EndpointSupplier#get(at.ac.univie.isc.asio.Language)
   */
  @Override
  public Endpoint get(final Language language) {
    final Type required = language.asEngineType();
    final DatasetEngine engine = available.get(required);
    if (engine == null) {
      final String message =
          String.format(Locale.ENGLISH, "unsupported query language : %s", language);
      throw new DatasetUsageException(message);
    }
    final FormatSelector negotiator = new FormatSelector(engine.supports(), new VariantConverter());
    final Engine acceptor = new AcceptorToEngineAdapter(new AsyncExecutor(transferFactory, engine), responseThreadPool);
    return new Endpoint(acceptor, parser, negotiator, new VariantConverter(), timeout);
  }
}

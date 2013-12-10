package at.ac.univie.isc.asio.frontend;

import java.util.Map;
import java.util.Set;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.coordination.EngineSpec;
import at.ac.univie.isc.asio.coordination.EngineSpec.Type;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class EngineSelector {

  private final Map<EngineSpec.Type, EngineAdapter> available;

  public EngineSelector(final Set<DatasetEngine> engines) {
    super();
    available = adaptEngines(engines);
  }

  private Map<Type, EngineAdapter> adaptEngines(final Set<DatasetEngine> engines) {
    final Builder<Type, EngineAdapter> product = ImmutableMap.<Type, EngineAdapter>builder();
    for (final DatasetEngine each : engines) {
      product.put(each.type(), EngineAdapter.adapt(each));
    }
    return product.build();
  }

  public Optional<EngineAdapter> select(final EngineSpec.Type type) {
    return Optional.fromNullable(available.get(type));
  }
}

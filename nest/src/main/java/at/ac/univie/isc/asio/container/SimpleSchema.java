package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.engine.Engine;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

@AutoValue
public abstract class SimpleSchema implements Schema {
  public static Schema create(final String name, final Engine... engines) {
    return new AutoValue_SimpleSchema(name, name, ImmutableSet.copyOf(engines));
  }

  @Override
  public void close() {}
}

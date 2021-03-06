/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.InvalidUsage;
import at.ac.univie.isc.asio.tool.Pair;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;

import javax.annotation.concurrent.ThreadSafe;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

@ThreadSafe
public final class TypeMatchingResolver<T> {

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  private final Map<MediaType, Canonical<T>> registered;

  private TypeMatchingResolver(final Map<MediaType, Canonical<T>> registered) {
    this.registered = registered;
  }

  public Selection<T> select(final Iterable<MediaType> accepted) {
    final MediaType selected = match(accepted);
    final Canonical<T> found = registered.get(selected);
    assert found != null : "matched key not registered";
    final T supplied = found.supplier().get();
    assert supplied != null : "supplier produced null";
    return new Selection<>(supplied, found.canonicalType());
  }

  private MediaType match(final Iterable<MediaType> accepted) {
    final Set<MediaType> available = registered.keySet();
    for (final MediaType candidate : accepted) {
      for (final MediaType key : available) {
        if (candidate.isCompatible(key)) {
          return key;
        }
      }
    }
    throw new NoMatchingFormat(accepted, available);
  }

  public static final class NoMatchingFormat extends InvalidUsage {
    public NoMatchingFormat(final Iterable<MediaType> accepted,
                            final Iterable<MediaType> registered) {
      super("no supported media type in " + accepted + " - expected one of " + registered);
    }
  }

  public static final class Builder<T> {
    private final ImmutableMap.Builder<MediaType, Canonical<T>> all;
    private Canonical<T> active = null;

    private Builder() {
      all = ImmutableMap.builder();
    }

    public Builder<T> register(final MediaType primaryType, final Supplier<? extends T> supplier) {
      requireNonNull(supplier);
      requireNonNull(primaryType);
      active = new Canonical<>(supplier, primaryType);
      all.put(primaryType, active);
      return this;
    }

    public Builder<T> alias(final MediaType aliasType) {
      assert active != null : "must register primary before aliasing";
      requireNonNull(aliasType);
      all.put(aliasType, active);
      return this;
    }

    public TypeMatchingResolver<T> make() {
      return new TypeMatchingResolver<>(all.build());
    }
  }

  private static final class Canonical<T> extends Pair<Supplier<? extends T>, MediaType> {
    private Canonical(final Supplier<? extends T> supplier, final MediaType mediaType) {
      super(supplier, mediaType);
    }

    public Supplier<? extends T> supplier() {
      return first();
    }

    public MediaType canonicalType() {
      return second();
    }
  }

  public static final class Selection<T> extends Pair<T, MediaType> {
    private Selection(final T selected, final MediaType type) {
      super(selected, type);
    }

    public T value() {
      return first();
    }

    public MediaType type() {
      return second();
    }
  }
}

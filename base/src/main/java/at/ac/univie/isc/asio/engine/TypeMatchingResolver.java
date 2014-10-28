package at.ac.univie.isc.asio.engine;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;
import javax.ws.rs.core.MediaType;

import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.tool.Pair;

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

  public static final class NoMatchingFormat extends DatasetUsageException {

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

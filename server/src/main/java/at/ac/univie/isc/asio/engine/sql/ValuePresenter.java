package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.AsioError;
import at.ac.univie.isc.asio.tool.Pretty;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Format an object value to a type specific string representation.
 */
final class ValuePresenter {
  public static final Function<Object, String> FAIL = new Function<Object, String>() {
    @Nullable
    @Override
    public String apply(@Nullable final Object input) {
      throw new NoRepresentationFound(input);
    }
  };

  public static Builder withDefault(final Function<Object, String> fallback) {
    return new Builder(fallback);
  }

  private final Map<Class<?>, Function<Object, String>> representations;
  private final Function<Object, String> fallback;

  private ValuePresenter(final Map<Class<?>, Function<Object, String>> representations,
                         final Function<Object, String> fallback) {
    this.representations = requireNonNull(representations);
    this.fallback = requireNonNull(fallback);
  }

  public String format(final Object value, final Class<?> type) {
    final Function<Object, String> representation;
    if (value == null) {
      representation = find(Void.class);
    } else {
      representation = find(type);
    }
    final String formatted = representation.apply(value);
    assert formatted != null : value + "("+ type +") formatted to null";
    return formatted;
  }

  private Function<Object, String> find(final Class<?> type) {
    final Function<Object, String> representation = representations.get(type);
    if (representation == null) {
      return fallback;
    }
    return representation;
  }

  static final class NoRepresentationFound extends AsioError.Base {
    protected NoRepresentationFound(final Object input) {
      super(Pretty.format("found no representation converter for %s", input));
    }
  }

  static final class Builder {
    private final Function<Object, String> fallback;
    private final ImmutableMap.Builder<Class<?>, Function<Object, String>> builder;

    private Builder(final Function<Object, String> fallback) {
      this.fallback = fallback;
      builder = ImmutableMap.builder();
    }

    public Builder register(final Function<Object, String> representation, final Class<?>... types) {
      for (final Class<?> each : types) {
        builder.put(each, representation);
      }
      return this;
    }

    public ValuePresenter build() {
      return new ValuePresenter(builder.build(), fallback);
    }
  }
}

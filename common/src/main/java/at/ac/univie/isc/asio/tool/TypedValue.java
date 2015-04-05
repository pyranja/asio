package at.ac.univie.isc.asio.tool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A string-convertible, immutable value type.
 * Implementations of this type should be transparent wrappers around a single value without
 * attached logic. The intention is to provide type safe primitive values, e.g. for identifiers.
 *
 * @param <VALUE> type of wrapped value
 */
@Immutable
public abstract class TypedValue<VALUE> {
  private final VALUE val;

  @JsonCreator
  protected TypedValue(@Nonnull final VALUE val) {
    requireNonNull(val, "id must not be null");
    this.val = normalize(val);
  }

  @JsonValue
  @Nonnull
  protected final VALUE value() {
    return val;
  }

  @Nonnull
  protected VALUE normalize(@Nonnull final VALUE val) {
    return val;
  }

  @Override
  public final String toString() {
    return val.toString();
  }

  @Override
  public final boolean equals(@Nullable final Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    final TypedValue that = (TypedValue) other;

    return val.equals(that.val);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(val, this.getClass());
  }
}

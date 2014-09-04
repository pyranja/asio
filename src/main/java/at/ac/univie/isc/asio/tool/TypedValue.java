package at.ac.univie.isc.asio.tool;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Immutable
public class TypedValue<VALUE> {
  @Nonnull private final VALUE val;

  protected TypedValue(@Nonnull final VALUE val) {
    requireNonNull(val, "id must not be null");
    this.val = normalize(val);
  }

  @Nonnull
  protected VALUE normalize(@Nonnull final VALUE val) {
    return val;
  }

  @Nonnull
  protected final VALUE value() {
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

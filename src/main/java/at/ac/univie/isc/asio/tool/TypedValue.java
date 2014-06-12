package at.ac.univie.isc.asio.tool;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Immutable
@Nonnull
public class TypedValue<VALUE> {
  private final VALUE val;

  protected TypedValue(final VALUE val) {
    requireNonNull(val, "id must not be null");
    this.val = normalize(val);
  }

  protected VALUE normalize(final VALUE val) {
    return val;
  }

  protected final VALUE value() {
    return val;
  }

  @Override
  public final String toString() {
    return val.toString();
  }

  @Override
  public final boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    final TypedValue that = (TypedValue) other;

    if (!val.equals(that.val)) {
      return false;
    }

    return true;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(val, this.getClass());
  }
}

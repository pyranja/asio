package at.ac.univie.isc.asio.tool;

import com.google.common.base.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public class Pair<FIRST, SECOND> {

  private final FIRST first;
  private final SECOND second;

  protected Pair(@Nonnull final FIRST first, @Nonnull final SECOND second) {
    this.first = requireNonNull(first);
    this.second = requireNonNull(second);
  }

  @Nonnull
  protected final FIRST first() {
    return first;
  }

  @Nonnull
  protected final SECOND second() {
    return second;
  }

  @Override
  public final String toString() {
    return Objects.toStringHelper(this)
        .addValue(first)
        .addValue(second)
        .toString();
  }

  @Override
  public final boolean equals(@Nullable final Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    final Pair pair = (Pair) other;

    if (first != null ? !first.equals(pair.first) : pair.first != null) {
      return false;
    }
    //noinspection RedundantIfStatement
    if (second != null ? !second.equals(pair.second) : pair.second != null) {
      return false;
    }

    return true;
  }

  @Override
  public final int hashCode() {
    return Objects.hashCode(first, second, this.getClass());
  }
}

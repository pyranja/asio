package at.ac.univie.isc.asio.tool;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Contain either a value or an error, that occurred while creating it.
 */
@Immutable
public final class ValueOrError<VALUE> {
  /**
   * Create an instance holding a value.
   * @param value to hold
   * @param <T> type of contained value
   * @return value bearing ValueOrError
   */
  public static <T> ValueOrError<T> valid(@Nonnull final T value) {
    Objects.nonNull(value);
    return new ValueOrError<>(value, null);
  }

  /**
   * Create an instance holding an error.
   * @param error to hold
   * @param <T> type of contained value
   * @return error bearing ValueOrError
   */
  public static <T> ValueOrError<T> invalid(@Nonnull final RuntimeException error) {
    Objects.nonNull(error);
    return new ValueOrError<>(null, error);
  }

  private final VALUE value;
  private final RuntimeException error;

  private ValueOrError(final VALUE value, final RuntimeException error) {
    assert ! (value == null && error == null) : "value and error are null";
    this.value = value;
    this.error = error;
  }

  /**
   * @return true if this holds a value
   */
  public boolean hasValue() {
    return error == null;
  }

  /**
   * @return true if this holds an error
   */
  public boolean hasError() {
    return error != null;
  }

  /**
   * @return contained error
   * @throws java.lang.IllegalStateException if no error is present
   */
  @Nonnull
  public RuntimeException error() {
    if (hasValue()) {
      throw new IllegalStateException("no error present");
    }
    return error;
  }

  /**
   * @return contained value
   * @throws java.lang.IllegalStateException if no value is present
   */
  @Nonnull
  public VALUE get() {
    if (hasError()) {
      throw new IllegalStateException("no value present");
    }
    return value;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    final ValueOrError that = (ValueOrError) o;

    if (error != null ? !error.equals(that.error) : that.error != null)
      return false;
    if (value != null ? !value.equals(that.value) : that.value != null)
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + (error != null ? error.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ValueOrError{" + "value=" + value + ", error=" + error + '}';
  }
}

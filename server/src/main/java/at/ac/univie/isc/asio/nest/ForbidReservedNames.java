package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.Id;
import com.google.common.collect.ImmutableSet;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;

/**
 * Prevent assembly of containers with names of system resource paths.
 */
public final class ForbidReservedNames implements Configurer {
  /**
   * Thrown if a container with a reserved name is assembled.
   */
  public static class IllegalContainerName extends IllegalArgumentException {
    public IllegalContainerName(final Id illegal) {
      super("'" + illegal + "' is a reserved name");
    }
  }

  private final Set<Id> reserved;

  public ForbidReservedNames(final Collection<Id> reserved) {
    this.reserved = ImmutableSet.copyOf(reserved);
  }

  @Nonnull
  @Override
  public NestConfig apply(final NestConfig input) {
    final Id name = input.getDataset().getName();
    if (reserved.contains(name)) { throw new IllegalContainerName(name); }
    return input;
  }

  @Override
  public String toString() {
    return "ForbidReservedNames{" +
        "reserved=" + reserved +
        '}';
  }
}

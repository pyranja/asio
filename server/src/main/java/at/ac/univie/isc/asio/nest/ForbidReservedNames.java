package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.AsioSettings;
import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.InvalidUsage;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;

/**
 * Prevent assembly of containers with names of system resource paths.
 */
@Brood
final class ForbidReservedNames implements Configurer {

  /**
   * Thrown if a container with a reserved name is assembled.
   */
  public static class IllegalContainerName extends InvalidUsage {
    public IllegalContainerName(final Id illegal) {
      super("'" + illegal + "' is a reserved name");
    }
  }

  private final Set<Id> reserved;

  @Autowired
  ForbidReservedNames(final AsioSettings config) {
    this(config.api.getReservedContainerNames());
  }

  ForbidReservedNames(final Collection<Id> reservedContainerNames) {
    this.reserved = ImmutableSet.copyOf(reservedContainerNames);
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

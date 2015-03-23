package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.metadata.BaseContainerRegistry;
import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Enumerate settings of active schemas.
 */
@Service
class SettingsRegistry extends BaseContainerRegistry {
  public SettingsRegistry() {
    log.info(Scope.SYSTEM.marker(), "settings registry enabled");
  }

  final Collection<Schema> findAll() {
    return ImmutableSet.copyOf(registry.keySet());
  }

  final ContainerSettings settingsOf(final Schema target) {
    return find(target).settings();
  }
}

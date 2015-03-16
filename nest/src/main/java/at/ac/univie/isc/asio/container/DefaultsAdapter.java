package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;

/**
 * Just wrap a given {@code ContainerSettings} with defaults and inject the schema name,
 * perform no further adaption.
 */
public final class DefaultsAdapter implements ContainerAdapter {
  public static DefaultsAdapter from(final ContainerSettings settings) {
    return new DefaultsAdapter(settings);
  }

  private final ContainerSettings settings;

  private DefaultsAdapter(final ContainerSettings given) {
    this.settings = ContainerSettings.copy(given);
  }

  @Override
  public ContainerSettings translate(final Schema schema, final ConfigStore store) {
    settings.setName(schema);
    return settings;
  }
}

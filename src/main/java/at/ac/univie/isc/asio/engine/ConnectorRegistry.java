package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Command;
import at.ac.univie.isc.asio.Connector;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.protocol.Parameters;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.security.Principal;
import java.util.Map;

/**
 * Delegate command creation to registered connectors based on the request language.
 */
public class ConnectorRegistry implements Connector {

  public static ConnectorRegistryBuilder builder() {
    return new ConnectorRegistryBuilder();
  }

  private final Map<Language, Connector> delegates;

  private ConnectorRegistry(final Map<Language, Connector> delegates) {
    this.delegates = delegates;
  }

  @Override
  public Command createCommand(final Parameters params, final Principal owner) {
    final Language language = params.language();
    final Connector delegate = delegates.get(language);
    if (delegate == null) {
      throw new LanguageNotSupported(language);
    } else {
      return delegate.createCommand(params, owner);
    }
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("connectors", delegates)
        .toString();
  }

  public static class ConnectorRegistryBuilder {
    private final ImmutableMap.Builder<Language, Connector> connectors =
        ImmutableMap.builder();

    public ConnectorRegistryBuilder add(final Language language, final Connector connector) {
      this.connectors.put(language, connector);
      return this;
    }

    public ConnectorRegistry build() {
      return new ConnectorRegistry(connectors.build());
    }
  }
}

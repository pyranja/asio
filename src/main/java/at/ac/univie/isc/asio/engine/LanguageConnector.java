package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Connector;
import at.ac.univie.isc.asio.Language;

/**
 * Marker interface for {@code Connector} implementations.
 */
public interface LanguageConnector extends Connector {
  Language language();
}

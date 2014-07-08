package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.Connector;
import at.ac.univie.isc.asio.Language;

public interface Registry {
  Connector find(Language language) throws Connector.LanguageNotSupported;

}

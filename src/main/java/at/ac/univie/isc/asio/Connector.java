package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.protocol.Parameters;

import java.security.Principal;

public interface Connector {
  Command createCommand(final Parameters params, Principal owner);

  final class LanguageNotSupported extends DatasetUsageException {
    public LanguageNotSupported(final Language language) {
      super(language + " is not supported");
    }
  }
}

package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.Connector;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Language;

public interface Registry {
  Connector find(Language language) throws LanguageNotSupported;

  public static final class LanguageNotSupported extends DatasetUsageException {
    public LanguageNotSupported(final Language language) {
      super(language + " is not supported");
    }
  }
}

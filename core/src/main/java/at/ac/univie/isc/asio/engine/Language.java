package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.tool.TypedValue;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * The query language accepted by an endpoint.
 */
public final class Language extends TypedValue<String> {

  /**
   * The given {@link at.ac.univie.isc.asio.engine.Language} is not supported by a component.
   */
  public static final class NotSupported extends DatasetUsageException {
    public NotSupported(final Language language) {
      super(language + " is not supported");
    }
  }

  public static final Language UNKNOWN = new Language("UNKNOWN");
  public static final Language SQL = new Language("SQL");
  public static final Language SPARQL = new Language("SPARQL");

  public static Language valueOf(final String value) {
    if (value == null || value.isEmpty()) {
      return UNKNOWN;
    } else {
      return new Language(value);
    }
  }

  Language(final String name) {
    super(name);
  }

  @Nonnull
  @Override
  protected String normalize(@Nonnull final String val) {
    return val.toUpperCase(Locale.ENGLISH).trim();
  }

  public String name() {
    return this.value();
  }
}

package at.ac.univie.isc.asio;

import java.util.Locale;

import at.ac.univie.isc.asio.coordination.EngineSpec.Type;

/**
 * The query language accepted by an endpoint.
 * 
 * @author Chris Borckholder
 */
public final class Language {

  public static final Language SQL = new Language("SQL", Type.SQL);
  public static final Language SPARQL = new Language("SPARQL", Type.SPARQL);
  // FIXME remove this
  public static final Language TEST = new Language("TEST", null);

  // TODO change to dynamic creation - let others handle rejection of unsupported language
  public static Language fromString(final String value) {
    final String name = value.toUpperCase(Locale.ENGLISH).trim();
    switch (name) {
      case "SQL":
        return SQL;
      case "SPARQL":
        return SPARQL;
      case "TEST": // FIXME remove this
        return TEST;
      default:
        throw new DatasetUsageException("unsupported language : " + value);
    }
  }

  private final String name;
  private final Type engine; // XXX remove when merging with Type enum

  public Language(final String name, final Type engine) {
    super();
    this.name = name;
    this.engine = engine;
  }

  public Type asEngineType() {
    return engine;
  }

  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return String.format("[lang|%s]", name);
  }
}

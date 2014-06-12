package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.tool.TypedValue;
import at.ac.univie.isc.asio.engine.EngineSpec.Type;

import java.util.Locale;

/**
 * The query language accepted by an endpoint.
 */
public final class Language extends TypedValue<String> {

  public static final Language SQL = new Language("SQL", Type.SQL);
  public static final Language SPARQL = new Language("SPARQL", Type.SPARQL);
  public static final Language TEST = new Language("TEST", null); // FIXME remove this

  // FIXME : simplify when EngineType is removed
  public static Language valueOf(final String value) {
    if (value == null) { return new Language("", null); }
    final String normalized = value.toUpperCase(Locale.ENGLISH).trim();
    switch (normalized) {
      case "SQL":
        return SQL;
      case "SPARQL":
        return SPARQL;
      case "TEST": // FIXME remove this
        return TEST;
      default:
        return new Language(value, null);
    }
  }

  private final Type engine; // FIXME : remove when merging with Type enum

  public Language(final String name, final Type engine) {
    super(name);
    this.engine = engine;
  }

  @Override
  protected String normalize(final String val) {
    return val.toUpperCase(Locale.ENGLISH).trim();
  }

  public Type asEngineType() {
    return engine;
  }

  public String name() {
    return this.value();
  }
}

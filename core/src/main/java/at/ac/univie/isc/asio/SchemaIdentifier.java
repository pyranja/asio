package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.tool.TypedValue;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Local identifier for deployed schemas.
 */
public final class SchemaIdentifier extends TypedValue<String> {
  /** by convention the name of the default schema */
  public static final SchemaIdentifier DEFAULT = SchemaIdentifier.valueOf("default");

  public static SchemaIdentifier valueOf(final String val) {
    return new SchemaIdentifier(val);
  }

  private SchemaIdentifier(final String val) {
    super(val);
  }

  /**
   * @return name of the schema
   */
  public String name() {
    return value();
  }

  @Nonnull
  @Override
  protected String normalize(@Nonnull final String val) {
    return val.toLowerCase(Locale.ENGLISH);
  }
}

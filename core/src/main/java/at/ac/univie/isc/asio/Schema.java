package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.tool.TypedValue;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Local identifier for deployed schemas.
 */
public final class Schema extends TypedValue<String> {
  /**
   * by convention the name of the default schema
   */
  public static final Schema DEFAULT = Schema.valueOf("default");


  /**
   * Thrown if a requested {@code Schema} is not deployed or not active.
   */
  public static final class NotFound extends RuntimeException {
    public NotFound(final Schema schema) {
      super(schema + " not found");
    }
  }

  public static Schema valueOf(final String val) {
    return new Schema(val);
  }

  private Schema(final String val) {
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

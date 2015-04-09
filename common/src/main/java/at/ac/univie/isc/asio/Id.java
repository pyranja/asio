package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.tool.TypedValue;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Local identifier for deployed schemas.
 * <p>
 * Ids are case insensitive and may only contain simple characters to enable safe usage in
 * {@code URIs}, file names and as identifiers in external systems.
 * Allowed characters are {@code a-z, A-Z, 0-9, _ and - }. A valid id must not start or end with
 * a hyphen and must not be empty. Identifier are always converted to lower case.
 * </p>
 */
public final class Id extends TypedValue<String> {
  /**
   * Pattern describing the allowed syntax of an {@code Id}.
   */
  static final Pattern SYNTAX = Pattern.compile("^[\\w]([\\w-]*[\\w])?$");

  /**
   * Thrown if a requested container is not deployed or not active.
   */
  public static final class NotFound extends InvalidUsage {
    public NotFound(final Id id) {
      super(id + " not found");
    }
  }

  /**
   * Thrown if a given string is not a valid {@code Id}.
   */
  public static final class IllegalIdentifier extends InvalidUsage {
    public IllegalIdentifier(final String raw) {
      super("<" + raw + "> is not a valid identifier");
    }
  }

  public static Id valueOf(final String val) {
    return new Id(val);
  }

  private Id(final String val) {
    super(val);
  }

  /**
   * @return name of the schema
   */
  public String asString() {
    return value();
  }

  @Nonnull
  @Override
  protected String normalize(@Nonnull final String val) {
    final String id = val.toLowerCase(Locale.ENGLISH);
    if (!SYNTAX.matcher(id).matches()) { throw new IllegalIdentifier(val); }
    return id;
  }
}

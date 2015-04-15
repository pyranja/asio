package at.ac.univie.isc.asio.munin;

import at.ac.univie.isc.asio.tool.TypedValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ServerStatus extends TypedValue<String> {
  public static final ServerStatus UP = ServerStatus.valueOf("UP");
  public static final ServerStatus DOWN = ServerStatus.valueOf("DOWN");

  @JsonCreator
  public static ServerStatus valueOf(@JsonProperty("status") final String val) {
    if (val == null) {
      return DOWN;
    } else {
      return new ServerStatus(val);
    }
  }

  private ServerStatus(@Nonnull final String val) {
    super(val);
  }

  @Nonnull
  @Override
  protected String normalize(@Nonnull final String val) {
    return val.toUpperCase(Locale.ENGLISH).trim();
  }
}

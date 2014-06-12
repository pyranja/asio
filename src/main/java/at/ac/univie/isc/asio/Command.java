package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.tool.TypedValue;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.transport.ObservableStream;
import rx.Observable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.ws.rs.core.MediaType;
import java.util.Locale;

/**
 *
 */
public interface Command {

  Id id();

  MediaType format();

  Role requiredRole();

  Observable<ObservableStream> observe();

  @Immutable
  @Nonnull
  public static final class Id extends TypedValue<String> {
    public static Id valueOf(final String input) {
      return new Id(input);
    }

    public Id(final String val) {
      super(val);
    }

    protected String normalize(final String val) {
      if (val.trim().isEmpty()) {
        throw new IllegalArgumentException("id must not be empty");
      }
      return val.trim().toLowerCase(Locale.ENGLISH);
    }
  }
}

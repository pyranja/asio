package at.ac.univie.isc.asio.engine;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;

import javax.annotation.concurrent.Immutable;
import javax.ws.rs.core.MediaType;
import java.security.Principal;
import java.util.List;

/**
 * A command in an invalid state. This does not contain any properties, but just the list of
 * violations. Invoking any method will trigger an exception, that indicates the cause of the
 * invalid state.
 */
@Immutable
final class InvalidCommand extends Command {
  private final RuntimeException cause;

  InvalidCommand(final RuntimeException cause) {
    this.cause = cause;
  }

  private RuntimeException fail() {
    throw cause;
  }

  @Override
  public void failIfNotValid() throws RuntimeException {
    throw fail();
  }

  @Override
  public String toString() {
    return "InvalidCommand{cause=" + cause + '}';
  }

  // === implementation fails fast on any gettter

  @Override
  public Multimap<String, String> properties() {
    throw fail();
  }

  @Override
  public List<MediaType> acceptable() {
    throw fail();
  }

  @Override
  public Optional<Principal> owner() {
    throw fail();
  }
}

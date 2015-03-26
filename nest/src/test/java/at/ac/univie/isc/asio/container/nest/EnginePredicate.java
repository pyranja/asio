package at.ac.univie.isc.asio.container.nest;

import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.Language;
import com.google.common.base.Predicate;

class EnginePredicate implements Predicate<Engine> {
  private final Language required;

  public EnginePredicate(final Language required) {
    this.required = required;
  }

  @Override
  public boolean apply(final Engine input) {
    return input.language() == required;
  }
}

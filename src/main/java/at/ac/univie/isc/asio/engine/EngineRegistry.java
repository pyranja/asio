package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.security.Role;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import rx.Observable;
import rx.Scheduler;

import java.security.Principal;
import java.util.Map;

public final class EngineRegistry implements Command.Factory {
  private final Map<Language, Engine> registry;
  private final Scheduler scheduler;

  public EngineRegistry(final Scheduler scheduler, final Iterable<Engine> engines) {
    this.scheduler = scheduler;
    this.registry = Maps.uniqueIndex(engines, new Function<Engine, Language>() {
      @Override
      public Language apply(final Engine input) {
        return input.language();
      }
    });
  }

  public Command accept(final Parameters params, final Principal owner) {
    final Engine delegate = registry.get(params.language());
    if (delegate == null) {
      throw new LanguageNotSupported(params.language());
    }
    final Invocation invocation = delegate.prepare(params, owner);
    return new ScheduledCommand(invocation, scheduler);
  }

  @VisibleForTesting
  static final class ScheduledCommand implements Command {
    private final Invocation handler;
    private final Scheduler scheduler;

    ScheduledCommand(final Invocation handler, final Scheduler scheduler) {
      this.handler = handler;
      this.scheduler = scheduler;
    }

    @Override
    public Role requiredRole() {
      return handler.requires();
    }

    @Override
    public Multimap<String, String> properties() {
      return handler.properties();
    }

    @Override
    public Observable<Results> observe() {
      return Observable
          .create(new OnSubscribeExecute(handler))
          .subscribeOn(scheduler);
    }

    @Override
    public String toString() {
      return handler.toString();
    }
  }
}

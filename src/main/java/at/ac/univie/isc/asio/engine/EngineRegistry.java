package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Command;
import at.ac.univie.isc.asio.Connector;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.protocol.Parameters;
import at.ac.univie.isc.asio.security.Role;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import rx.Observable;
import rx.Scheduler;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.security.Principal;
import java.util.Map;

public final class EngineRegistry implements Connector {
  private final Map<Language, Engine> registry;
  private final Scheduler scheduler;

  public EngineRegistry(final Scheduler scheduler, final Iterable<Engine> engines) {
    this.scheduler = scheduler;
    this.registry = Maps.uniqueIndex(engines, new Function<Engine, Language>() {
      @Nullable
      @Override
      public Language apply(@Nullable final Engine input) {
        return input.language();
      }
    });
  }

  @Override
  public Command createCommand(final Parameters params, final Principal owner) {
    final Engine delegate = registry.get(params.language());
    if (delegate == null) {
      throw new LanguageNotSupported(params.language());
    }
    final Invocation invocation = delegate.create(params, owner);
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
    public MediaType format() {
      throw new UnsupportedOperationException("deprecated");
    }

    @Override
    public Role requiredRole() {
      return handler.requires();
    }

    @Override
    public Observable<Results> observe() {
      return Observable
          .create(new OnSubscribeExecute(handler))
          .subscribeOn(scheduler);
    }
  }
}

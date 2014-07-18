package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.Command;
import at.ac.univie.isc.asio.Connector;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.engine.ReactiveOperationExecutor;
import at.ac.univie.isc.asio.engine.OperationAdapter;
import at.ac.univie.isc.asio.engine.OperationFactory;

import java.security.Principal;

public final class CommandFactory implements Connector {
  private static final ActionExtractor EXTRACTOR = new ActionExtractor();

  private final FormatMatcher matcher;
  private final ReactiveOperationExecutor backend;
  private final OperationFactory create;

  public CommandFactory(final FormatMatcher matcher, final ReactiveOperationExecutor backend, final OperationFactory create) {
    this.matcher = matcher;
    this.backend = backend;
    this.create = create;
  }

  @Override
  public Command createCommand(final Parameters params, Principal owner) {
    final FormatMatcher.FormatAndMediaType formatAndMediaType = matcher.match(params.acceptable());
    final ActionExtractor.ActionAndCommand parsed = EXTRACTOR.findActionIn(params.props());
    final DatasetOperation operation = create
        .fromAction(parsed.action, parsed.command)
        .renderAs(formatAndMediaType.format)
        .withOwner(owner);
    return new OperationAdapter(formatAndMediaType.type, operation, backend);
  }
}

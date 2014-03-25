package at.ac.univie.isc.asio.protocol;

import static java.lang.String.format;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.frontend.OperationFactory;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

/**
 * Convert request parameters into a {@link DatasetOperation}
 * 
 * @author Chris Borckholder
 */
public final class OperationParser {

  private final OperationFactory create;

  public OperationParser(final OperationFactory create) {
    super();
    this.create = create;
  }

  // struct
  private static final class ActionAndCommand {
    public final Action action;
    public final String command;

    public ActionAndCommand(final Action action, final String command) {
      super();
      this.action = action;
      this.command = command;
    }
  }

  /**
   * Use the given parameters to create an operation. Fail if (1) no action is specified in the
   * parameters, (2) multiple actions are specified, (3) the action has no associated command, (4)
   * the action is not contained in the set of allowed actions.
   * 
   * @param parameters mapping of request parameters and values
   * @param allowed set of accepted actions
   * @return initialized operation
   * @throws DatasetUsageException if no operation can be created from given parameters.
   */
  OperationBuilder operationFromParameters(final Map<String, List<String>> parameters,
      final Set<Action> allowed) throws DatasetUsageException {
    final ActionAndCommand found = findActionIn(parameters);
    if (allowed.contains(found.action)) {
      return create.fromAction(found.action, found.command);
    } else {
      final String message =
          format(Locale.ENGLISH, "found %s but expected one of %s", found.action, allowed);
      throw new DatasetUsageException(message);
    }
  }

  OperationBuilder operationForAction(final Action action) {
    return create.fromAction(action, null);
  }

  /**
   * Find the single action and associated command in the given parameter map. Fail if no or more
   * than one actions are present.
   * 
   * @param parameters
   * @return action and associated command
   */
  private ActionAndCommand findActionIn(final Map<String, List<String>> parameters) {
    ActionAndCommand found = null;
    for (final String each : parameters.keySet()) {
      final Optional<Action> action = actionFromString(each);
      if (action.isPresent()) {
        failOnMultiple(found, action);
        final String command = extractSingleCommandOrFail(each, parameters);
        found = new ActionAndCommand(action.get(), command);
      }
    }
    if (found == null) {
      throw new DatasetUsageException("found no valid action parameter");
    }
    return found;
  }

  /**
   * @param found
   * @param action
   */
  private void failOnMultiple(final ActionAndCommand found, final Optional<Action> action) {
    if (found != null) {
      final String message =
          format(Locale.ENGLISH, "found multiple actions: %s <> %s", found.action, action.get());
      throw new DatasetUsageException(message);
    }
  }

  /**
   * @param key action name as present in parameter map
   * @param params map of parameters
   * @return the single command text associated with given action key
   * @throws DatasetUsageException if none or more than one command is associated with given key
   */
  private String extractSingleCommandOrFail(final String key, final Map<String, List<String>> params) {
    final List<String> commands = params.get(key);
    if (commands == null || commands.isEmpty()) {
      final String message = format(Locale.ENGLISH, "missing command for %s", key);
      throw new DatasetUsageException(message);
    }
    if (commands.size() > 1) {
      final String message = format(Locale.ENGLISH, "more than one command for %s", key);
      throw new DatasetUsageException(message);
    }
    return Iterables.getOnlyElement(commands);
  }

  /**
   * Convert the given string into the matching action if possible.
   * 
   * @param text to be converted
   * @return an Optional holding the parsed action
   */
  private Optional<Action> actionFromString(final String text) {
    final String normalized = text.trim().toUpperCase(Locale.ENGLISH);
    for (final Action each : Action.values()) {
      if (normalized.equals(each.name())) {
        return Optional.of(Action.valueOf(normalized));
      }
    }
    return Optional.absent();
  }
}

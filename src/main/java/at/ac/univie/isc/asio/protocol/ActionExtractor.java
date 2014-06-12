package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetUsageException;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

import java.util.List;
import java.util.Locale;
import java.util.Map;

// FIXME : replace with engine specific logic
public class ActionExtractor {
  /**
   * Find the single action and associated command in the given parameter map. Fail if no or more
   * than one actions are present.
   *
   * @param parameters
   * @return action and associated command
   */
  ActionAndCommand findActionIn(final Map<String, List<String>> parameters) {
    ActionAndCommand found = null;
    for (final String each : parameters.keySet()) {
      final Optional<DatasetOperation.Action> action = actionFromString(each);
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

  private void failOnMultiple(final ActionAndCommand found, final Optional<DatasetOperation.Action> action) {
    if (found != null) {
      final String message =
          String.format(Locale.ENGLISH, "found multiple actions: %s <> %s", found.action, action.get());
      throw new DatasetUsageException(message);
    }
  }

  /**
   * @param key    action name as present in parameter map
   * @param params map of parameters
   * @return the single command text associated with given action key
   * @throws at.ac.univie.isc.asio.DatasetUsageException if none or more than one command is associated with given key
   */
  private String extractSingleCommandOrFail(final String key, final Map<String, List<String>> params) {
    final List<String> commands = params.get(key);
    if (commands == null || commands.isEmpty()) {
      final String message = String.format(Locale.ENGLISH, "missing command for %s", key);
      throw new DatasetUsageException(message);
    }
    if (commands.size() > 1) {
      final String message = String.format(Locale.ENGLISH, "more than one command for %s", key);
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
  private Optional<DatasetOperation.Action> actionFromString(final String text) {
    final String normalized = text.trim().toUpperCase(Locale.ENGLISH);
    for (final DatasetOperation.Action each : DatasetOperation.Action.values()) {
      if (normalized.equals(each.name())) {
        return Optional.of(DatasetOperation.Action.valueOf(normalized));
      }
    }
    return Optional.absent();
  }

  // struct
  public static final class ActionAndCommand {
    public final DatasetOperation.Action action;
    public final String command;

    public ActionAndCommand(final DatasetOperation.Action action, final String command) {
      this.action = action;
      this.command = command;
    }
  }
}

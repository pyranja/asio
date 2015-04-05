package at.ac.univie.isc.asio.tool;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Collect errors, e.g. while validating an object.
 */
public final class Violations {
  private final List<String> errors = new ArrayList<>();
  private boolean fatal = false;

  public static Violations newInstance() {
    return new Violations();
  }

  private Violations() {}

  /**
   * Record the given violation as a non-fatal, informational warning.
   */
  public void warn(final String violation) {
    errors.add("[WARNING] " + violation);
  }

  /**
   * Record the given violation, that invalidates the whole activity.
   */
  public void fail(final String violation) {
    fatal = true;
    errors.add("[FATAL] " + violation);
  }

  /**
   * True if up to now there were no fatal violations.
   */
  public boolean currentlyValid() {
    return !fatal;
  }

  /**
   * True if at least one fatal violation was reported.
   */
  public boolean isFatal() {
    return fatal;
  }

  /**
   * True if there are violations, but all are non-fatal.
   */
  public boolean hasWarnings() {
    return !fatal && errors.isEmpty();
  }

  /**
   * Snapshot of currently collected violations.
   */
  public List<String> getViolations() {
    return ImmutableList.copyOf(errors);
  }

  @Override
  public String toString() {
    return "Violations{" +
        "fatal=" + fatal +
        ", errors=" + errors +
        '}';
  }
}

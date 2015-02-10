package at.ac.univie.isc.asio.junit;

import at.ac.univie.isc.asio.Pretty;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Collect reports from registered components and add them to the failure description if a test fails.
 */
public final class Interactions implements TestRule {

  /** factory method */
  public static Interactions empty() {
    return new Interactions();
  }

  /**
   * A text report, summarizing some activity.
   */
  public static interface Report {
    /**
     * Append this report to the given sink.
     *
     * @param sink to write to
     * @return the sink
     * @throws java.io.IOException if appending fails
     */
    Appendable appendTo(final Appendable sink) throws IOException;
  }

  private final Set<Report> reports = new HashSet<>();

  private Interactions() {}

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          base.evaluate();
        } catch (AssumptionViolatedException skipMe) {
          throw skipMe;
        } catch (AssertionError failure) {
          throw new TestFailedReport(collectReports(), failure);
        } catch (Throwable error) {
          throw new TestInErrorReport(collectReports(), error);
        }
      }
    };
  }

  /**
   * @param report to be collected
   * @return this
   */
  public Interactions and(final Report report) {
    this.reports.add(report);
    return this;
  }

  private String collectReports() {
    final StringBuilder collector = new StringBuilder();
    collector.append(Pretty.justify(" RECORDED INTERACTIONS ", 75, '#')).append(System.lineSeparator());
    for (Report report : reports) {
      try {
        collector.append(Pretty.justify(' ' + report.toString() + ' ', 75, '#')).append(System.lineSeparator());
        report.appendTo(collector);
        collector.append(System.lineSeparator());
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    collector.append(Pretty.justify(" END OF INTERACTIONS ", 75, '#')).append(System.lineSeparator());
    return collector.toString();
  }

  /**
   * Enrich a test error with additional details.
   */
  static class TestInErrorReport extends RuntimeException {
    TestInErrorReport(final String message, final Throwable error) {
      super(error.getClass().getName() + ": " + merge(message, error));
      this.setStackTrace(error.getStackTrace());
      this.addSuppressed(error);
    }
  }


  /**
   * Enrich a test failure with additional details.
   */
  static class TestFailedReport extends AssertionError {
    TestFailedReport(final String message, final Throwable failure) {
      super(merge(message, failure));
      this.setStackTrace(failure.getStackTrace());
      this.addSuppressed(failure);
    }
  }

  private static String merge(final String message, final Throwable error) {
    return Pretty.format("%n%s%s", message, error.getMessage());
  }
}

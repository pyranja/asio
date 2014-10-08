package at.ac.unvie.isc.asio.junit;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Collect reports from registered components and add them to the failure description if a test fails.
 */
public final class ReportCollector implements TestRule {
  /**
   * @return a new ReportCollector
   */
  public static ReportCollector register(final Report... reports) {
    final ReportCollector instance = new ReportCollector();
    for (final Report each : reports) { instance.and(each); }
    return instance;
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

  private ReportCollector() {}

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
  public ReportCollector and(final Report report) {
    this.reports.add(report);
    return this;
  }

  private String collectReports() {
    final StringBuilder collector = new StringBuilder();
    for (Report report : reports) {
      try {
        report.appendTo(collector);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
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
    return String.format(Locale.ENGLISH, "%s%n%s", error.getMessage(), message);
  }
}

package asio;

import at.ac.univie.isc.asio.acceptance.*;
import junit.runner.Version;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * Run acceptance tests.
 */
public final class Check {
  private static final Class<?>[] TESTS = new Class[] {
      MetadataTest.class, EventStreamTest.class, PermissionTest.class, SchemaTest.class,
      SparqlDatetime.class, SparqlFederationTest.class, SparqlModesTest.class, SparqlQueryTest.class
      , SqlQueryTest.class, SqlUpdateTest.class
  };

  public static void main(String[] args) {
    System.out.println("JUnit version "+ Version.id());
    final JUnitCore runner = new JUnitCore();
    runner.addListener(new TextListener(System.out));
    final Result run = runner.run(TESTS);
    System.out.println("Tests run : " + run.getRunCount() + " Failures : "+ run.getFailureCount() + " Ignored : "+ run.getIgnoreCount());
    System.out.println("Time taken : " + run.getRunTime() + "ms");
    for (final Failure failure : run.getFailures()) {
      System.err.println(failure);
    }
    System.exit(run.wasSuccessful() ? 0 : 1);
  }
}

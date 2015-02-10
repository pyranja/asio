package at.ac.univie.isc.asio.junit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Aggregate multiple reports.
 */
public final class CompositeReport implements Interactions.Report {
  private final Collection<Interactions.Report> reports;

  private CompositeReport() {
    reports = Collections.synchronizedList(new ArrayList<Interactions.Report>());
  }

  public static CompositeReport create() {
    return new CompositeReport();
  }

  public CompositeReport attach(final Interactions.Report report) {
    reports.add(report);
    return this;
  }

  @Override
  public Appendable appendTo(final Appendable sink) throws IOException {
    synchronized (reports) {
      if (reports.isEmpty()) {
        sink.append("EMPTY");
      } else {
        for (Interactions.Report report : reports) {
          report.appendTo(sink);
        }
      }
    }
    return sink;
  }

  @Override
  public String toString() {
    return "Composite" + reports;
  }
}

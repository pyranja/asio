/*
 * #%L
 * asio test
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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

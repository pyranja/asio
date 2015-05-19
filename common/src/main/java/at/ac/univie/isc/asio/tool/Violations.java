/*
 * #%L
 * asio common
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

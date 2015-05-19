/*
 * #%L
 * asio server
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
package at.ac.univie.isc.asio.insight;

import javax.annotation.Nullable;
import java.sql.SQLException;

/**
 * Factory for events from sql executions.
 */
public final class Sql {
  private Sql() { /** static factory */ }

  public static SqlEvent success(final String statement) {
    return new SqlEvent("success", statement, null);
  }

  public static SqlEvent failure(final String statement, final SQLException error) {
    return new SqlEvent("failure", statement, error.getMessage());
  }

  public static class SqlEvent extends Event {
    private final String statement;
    private final String error;
    private long duration = 0;  // in ms
    private boolean noIndex = false;
    private boolean badIndex = false;

    SqlEvent(final String subject, final String statement, @Nullable final String error) {
      super("sql", subject);
      this.statement = statement;
      this.error = error;
    }

    public String getStatement() {
      return statement;
    }

    public String getError() {
      return error;
    }

    public long getDuration() {
      return duration;
    }

    public void setDuration(final long duration) {
      this.duration = duration;
    }

    public boolean isNoIndex() {
      return noIndex;
    }

    public void setNoIndex(final boolean noIndex) {
      this.noIndex = noIndex;
    }

    public boolean isBadIndex() {
      return badIndex;
    }

    public void setBadIndex(final boolean badIndex) {
      this.badIndex = badIndex;
    }
  }
}

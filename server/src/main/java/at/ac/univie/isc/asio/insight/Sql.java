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

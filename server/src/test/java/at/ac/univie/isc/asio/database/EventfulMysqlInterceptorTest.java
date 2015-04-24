package at.ac.univie.isc.asio.database;

import at.ac.univie.isc.asio.insight.Emitter;
import at.ac.univie.isc.asio.insight.Event;
import at.ac.univie.isc.asio.insight.Sql;
import com.google.common.testing.FakeTicker;
import com.mysql.jdbc.PreparedStatement;
import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static at.ac.univie.isc.asio.tool.EventMatchers.hasSubject;
import static at.ac.univie.isc.asio.tool.EventMatchers.hasType;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(HierarchicalContextRunner.class)
public class EventfulMysqlInterceptorTest {
  private final ArgumentCaptor<Sql.SqlEvent> captured = ArgumentCaptor.forClass(Sql.SqlEvent.class);

  private final Emitter events = Mockito.mock(Emitter.class);
  private final FakeTicker time = new FakeTicker();
  private final EventfulMysqlInterceptor subject = new EventfulMysqlInterceptor(time, events);

  @Before
  public void startQuerySimulation() throws SQLException {
    //    EventfulMysqlInterceptor.sharedEmitterInstance = events;
    // pre-process should ignore all input - no need to change call in tests
    subject.preProcess(null, null, null);
  }

  @After
  public void cleanUp() {
    new EventfulMysqlInterceptor.Wiring().resetSharedEmitter();
  }

  @Test
  public void should_use_emitter_from_global_handover_variable() throws Exception {
    final EventfulMysqlInterceptor selfInitializing = new EventfulMysqlInterceptor();
    assertThat(selfInitializing.events(), not(sameInstance(events)));
    EventfulMysqlInterceptor.sharedEmitterInstance = events;
    assertThat(selfInitializing.events(), sameInstance(events));
  }

  @Test
  public void should_only_intercept_top_level_queries() throws Exception {
    assertThat(subject.executeTopLevelOnly(), equalTo(true));
  }

  @Test
  public void should_emit_flags_if_set() throws Exception {
    subject.postProcess(null, null, null, null, 0, true, true, null);
    assertThat(emitted().isNoIndex(), equalTo(true));
    assertThat(emitted().isBadIndex(), equalTo(true));
  }

  @Test
  public void should_not_fail_on_unusual_input() throws Exception {
    subject.postProcess(null, null, null, null, Integer.MIN_VALUE, false, true, null);
  }

  @Test
  public void should_emit_duration_of_sql_execution() throws Exception {
    time.advance(TimeUnit.MILLISECONDS.toNanos(143));
    subject.postProcess(null, null, null, null, Integer.MIN_VALUE, false, true, null);
    assertThat(emitted().getDuration(), equalTo(143L));
  }

  @Test
  public void should_suppress_queries_marked_as_driver_internal_ones() throws Exception {
    subject.postProcess("/* mysql-connector-java-5.1.34 */ SELECT * FROM test;", null, null, null, Integer.MIN_VALUE, false, true, null);
    verify(events, never()).emit(any(Event.class));
  }

  public class IfHandlingPreparedStatement {
    private final PreparedStatement stmt = Mockito.mock(PreparedStatement.class);

    @Test
    public void should_emit_executed_statement_on_success() throws Exception {
      given(stmt.asSql()).willReturn("sql-prepared_statement");
      subject.postProcess(null, stmt, null, null, 0, false, false, null);
      assertThat(emitted().getStatement(), equalTo("sql-prepared_statement"));
    }

    @Test
    public void should_emit_executed_statement_on_failure() throws Exception {
      given(stmt.asSql()).willReturn("sql-prepared_statement");
      subject.postProcess(null, stmt, null, null, 0, false, false, new SQLException());
      assertThat(emitted().getStatement(), equalTo("sql-prepared_statement"));
    }
  }


  public class OnSuccess {
    @Before
    public void simulateSuccessfulQuery() throws SQLException {
      subject.postProcess("sql-statement", null, null, null, 0, false, false, null);
    }

    @Test
    public void should_emit_sql_event() throws Exception {
      verify(events).emit(argThat(hasType("sql")));
    }

    @Test
    public void should_emit_success() throws Exception {
      verify(events).emit(argThat(hasSubject("success")));
    }

    @Test
    public void should_emit_executed_statement() throws Exception {
      assertThat(emitted().getStatement(), equalTo("sql-statement"));
    }

    @Test
    public void should_emit_no_error() throws Exception {
      assertThat(emitted().getError(), nullValue());
    }

    @Test
    public void should_emit_flags() throws Exception {
      assertThat(emitted().isBadIndex(), equalTo(false));
      assertThat(emitted().isNoIndex(), equalTo(false));
    }
  }


  public class OnFailure {
    @Before
    public void simulateFailingQuery() throws SQLException {
      subject.postProcess("sql-statement", null, null, null, 0, false, false, new SQLException("reason"));
    }

    @Test
    public void should_emit_sql_event() throws Exception {
      verify(events).emit(argThat(hasType("sql")));
    }

    @Test
    public void should_emit_failure() throws Exception {
      verify(events).emit(argThat(hasSubject("failure")));
    }

    @Test
    public void should_emit_error_message() throws Exception {
      assertThat(emitted().getError(), equalTo("reason"));
    }

    @Test
    public void should_emit_executed_statement() throws Exception {
      assertThat(emitted().getStatement(), equalTo("sql-statement"));
    }

    @Test
    public void should_emit_flags() throws Exception {
      assertThat(emitted().isBadIndex(), equalTo(false));
      assertThat(emitted().isNoIndex(), equalTo(false));
    }
  }

  private Sql.SqlEvent emitted() {
    verify(events).emit(captured.capture());
    return captured.getValue();
  }
}

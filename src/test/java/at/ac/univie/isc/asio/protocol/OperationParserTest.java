package at.ac.univie.isc.asio.protocol;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Set;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.common.IdGenerator;
import at.ac.univie.isc.asio.frontend.OperationFactory;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;

import com.google.common.collect.ImmutableSet;

@RunWith(Theories.class)
public class OperationParserTest {

  private static final Set<Action> ALLOWED = ImmutableSet.of(Action.QUERY, Action.UPDATE);

  private final IdGenerator ids = new IdGenerator() {
    @Override
    public String next() {
      return "test";
    }
  };

  private final OperationParser subject = new OperationParser(new OperationFactory(ids));

  @Test
  public void should_find_single_allowed_action() throws Exception {
    final MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
    params.add("query", "command");
    final OperationBuilder op = subject.operationFromParameters(params, ALLOWED);
    assertThat(op.getAction(), is(Action.QUERY));
    assertThat(op.getCommand(), is("command"));
  }

  @DataPoints
  public static Object[] params() {
    return new Object[] {
    /* empty */newMap(),
    /* no action */newMap("not_an_action", "command"),
    /* duplicated command */newMap("query", "first", "query", "duplicate"),
    /* more than one action */newMap("query", "first", "update", "second"),
    /* action not allowed */newMap("schema", "command")};
  }

  private static MultivaluedMap<String, String> newMap(final String... content) {
    final MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
    for (int i = 0; i < content.length; i += 2) {
      map.add(content[i], content[i + 1]);
    }
    return map;
  }

  @Theory
  public void should_fail(final MultivaluedMap<String, String> params) {
    try {
      subject.operationFromParameters(params, ALLOWED);
      fail("should have failed for " + params);
    } catch (final DatasetUsageException e) {
      // expected
    }
  }
}

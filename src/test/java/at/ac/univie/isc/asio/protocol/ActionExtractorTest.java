package at.ac.univie.isc.asio.protocol;


import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetUsageException;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class ActionExtractorTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final ActionExtractor subject = new ActionExtractor();

  @Test
  public void should_find_single_action() throws Exception {
    final Map<String, List<String>> params =
        Multimaps.asMap(ImmutableListMultimap.of("query", "command"));
    final ActionExtractor.ActionAndCommand parsed = subject.findActionIn(params);
    assertThat(parsed.action, is(Action.QUERY));
    assertThat(parsed.command, is("command"));
  }

  @DataPoints
  public static Object[] params() {
    return new Object[] {
    /* empty */newMap(),
    /* no action */newMap("not_an_action", "command"),
    /* duplicated command */newMap("query", "first", "query", "duplicate"),
    /* more than one action */newMap("query", "first", "update", "second"),
    /* empty command */newMap("query", null)};
  }

  private static MultivaluedMap<String, String> newMap(final String... content) {
    final MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
    for (int i = 0; i < content.length; i += 2) {
      map.add(content[i], content[i + 1]);
    }
    return map;
  }

  @Theory
  public void illegal_parameters(final MultivaluedMap<String, String> params) {
    error.expect(DatasetUsageException.class);
    subject.findActionIn(params);
  }
}

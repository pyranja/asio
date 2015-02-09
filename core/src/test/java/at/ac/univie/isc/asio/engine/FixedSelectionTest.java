package at.ac.univie.isc.asio.engine;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class FixedSelectionTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final Engine sql = Mockito.mock(Engine.class);
  private final Engine sparql = Mockito.mock(Engine.class);

  private FixedSelection subject;

  @Before
  public void setupMockEngines() {
    when(sql.language()).thenReturn(Language.SQL);
    when(sparql.language()).thenReturn(Language.SPARQL);
    subject = FixedSelection.from(ImmutableSet.of(sql, sparql));
  }

  @Test
  public void should_yield_single_engine_that_supports_language_of_given_command() throws Exception {
    final Engine selected = subject.select(CommandBuilder.with(Language.SQL).build());
    assertThat(selected, is(sql));
  }

  @Test
  public void should_fail_if_no_engine_supports_given_command() throws Exception {
    error.expect(Language.NotSupported.class);
    subject.select(CommandBuilder.with(Language.UNKNOWN).build());
  }

  @Test
  public void should_fail_fast_if_constructed_with_multiple_engines_for_a_single_engine() throws Exception {
    final Engine duplicate = Mockito.mock(Engine.class);
    when(duplicate.language()).thenReturn(Language.SQL);
    error.expect(IllegalArgumentException.class);
    FixedSelection.from(ImmutableSet.of(sql, duplicate));
  }
}

package at.ac.univie.isc.asio.engine;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SelectByLanguageTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final Engine delegate = Mockito.mock(Engine.class);
  private final SelectByLanguage subject = new SelectByLanguage(ImmutableMap.of(Language.SQL, delegate));

  @Test
  public void should_fail_if_no_engine_for_language_found() throws Exception {
    error.expect(Language.NotSupported.class);
    subject.prepare(ParametersBuilder.with(Language.UNKNOWN).build());
  }

  @Test
  public void should_delegate_parameters_to_mapped_engine() throws Exception {
    final Parameters params = ParametersBuilder.with(Language.SQL).build();
    subject.prepare(params);
    verify(delegate).prepare(params);
  }

  @Test
  public void should_yield_the_delegates_invocation() throws Exception {
    final Invocation expected = Mockito.mock(Invocation.class);
    final Parameters params = ParametersBuilder.with(Language.SQL).build();
    when(delegate.prepare(params)).thenReturn(expected);
    final Invocation actual = subject.prepare(params);
    assertThat(actual, is(expected));
  }
}

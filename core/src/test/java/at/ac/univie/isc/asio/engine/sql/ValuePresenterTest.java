package at.ac.univie.isc.asio.engine.sql;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ValuePresenterTest {

  private Representation representation;
  private ValuePresenter subject;

  @Before
  public void setUp() throws Exception {
    representation = Mockito.mock(Representation.class);
    when(representation.apply(any())).thenReturn("test");
  }

  @Test
  public void should_use_registered_function_for_value() throws Exception {
    subject = ValuePresenter.withDefault(ValuePresenter.FAIL).register(representation, Long.class).build();
    subject.format(1L, Long.class);
    verify(representation).apply(1L);
  }

  @Test
  public void should_yield_registered_functions_return_value() throws Exception {
    subject = ValuePresenter.withDefault(ValuePresenter.FAIL).register(representation, Long.class).build();
    final String actual = subject.format(1L, Long.class);
    assertThat(actual, is("test"));
  }

  @Test
  public void should_use_void_representation_for_null_value() throws Exception {
    subject = ValuePresenter.withDefault(ValuePresenter.FAIL).register(representation, Void.class).build();
    subject.format(null, Object.class);
    verify(representation).apply(null);
  }

  @Test
  public void fall_back_to_default_representation() throws Exception {
    subject = ValuePresenter.withDefault(representation).build();
    final String formatted = subject.format("fallback", Object.class);
    assertThat(formatted, is("test"));
  }

  @Test(expected = ValuePresenter.NoRepresentationFound.class)
  public void fail_if_no_representation_found() throws Exception {
    subject = ValuePresenter.withDefault(ValuePresenter.FAIL).build();
    subject.format(new Object(), Object.class);
  }

  @Test(expected = AssertionError.class)
  public void fail_if_representation_yields_null() throws Exception {
    when(representation.apply(any())).thenReturn(null);
    subject = ValuePresenter.withDefault(ValuePresenter.FAIL).register(representation, Object.class).build();
    subject.format(new Object(), Object.class);
  }
}

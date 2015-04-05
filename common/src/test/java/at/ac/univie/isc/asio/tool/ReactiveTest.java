package at.ac.univie.isc.asio.tool;

import com.google.common.base.Optional;
import org.junit.Test;
import rx.Observable;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ReactiveTest {
  @Test
  public void should_yield_absent_if_source_observable_is_empty() throws Exception {
    assertThat(Reactive.asOptional(Observable.<String>empty()), equalTo(Optional.<String>absent()));
  }

  @Test
  public void should_yield_single_element_of_sequence() throws Exception {
    assertThat(Reactive.asOptional(Observable.just("test")).get(), equalTo("test"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_if_source_sequence_has_multiple_elements() throws Exception {
    Reactive.asOptional(Observable.from("one", "two"));
  }
}

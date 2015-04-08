package at.ac.univie.isc.asio.insight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.ClassSanityTester;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ErrorTest {
  @Test
  public void sane_class() throws Exception {
    new ClassSanityTester()
        .setSampleInstances(Throwable.class, Arrays.asList(new RuntimeException("exception-one"), new RuntimeException("exception-two")))
        .setSampleInstances(Correlation.class, Arrays.asList(Correlation.valueOf("one"), Correlation.valueOf("two")))
        .forAllPublicStaticMethods(Error.class)
        .testEquals()
    ;
  }

  @Test
  public void jackson_round_tripping() throws Exception {
    final Correlation correlation = Correlation.valueOf("correlation");
    final Error.ErrorChainElement first =
        Error.ErrorChainElement.create("first-exception", "first-location");
    final Error.ErrorChainElement second =
        Error.ErrorChainElement.create("second-exception", "second-location");
    final Error original =
        Error.create("message", "cause", correlation, 1337, ImmutableList.of(first, second));
    final ObjectMapper mapper = new ObjectMapper();
    final String json = mapper.writeValueAsString(original);
    final Error read = mapper.readValue(json, Error.class);
    assertThat(read, is(original));
  }

  @Test(timeout = 1_000L)
  public void do_not_fail_on_circular_causal_chain() throws Exception {
    final RuntimeException top = new RuntimeException("top");
    final RuntimeException circular = new RuntimeException("circular");
    top.initCause(circular);
    circular.initCause(top);
    Error.from(top, Correlation.valueOf("none"), -1L, true);
  }
}

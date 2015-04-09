package at.ac.univie.isc.asio.insight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.ClassSanityTester;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class VndErrorTest {
  @Test
  public void sane_class() throws Exception {
    new ClassSanityTester()
        .setSampleInstances(Throwable.class, Arrays.asList(new RuntimeException("exception-one"), new RuntimeException("exception-two")))
        .setSampleInstances(Correlation.class, Arrays.asList(Correlation.valueOf("one"), Correlation.valueOf("two")))
        .forAllPublicStaticMethods(VndError.class)
        .testEquals()
    ;
  }

  @Test
  public void jackson_round_tripping() throws Exception {
    final Correlation correlation = Correlation.valueOf("correlation");
    final VndError.ErrorChainElement first =
        VndError.ErrorChainElement.create("first-exception", "first-location");
    final VndError.ErrorChainElement second =
        VndError.ErrorChainElement.create("second-exception", "second-location");
    final VndError original =
        VndError.create("message", "cause", correlation, 1337, ImmutableList.of(first, second));
    final ObjectMapper mapper = new ObjectMapper();
    final String json = mapper.writeValueAsString(original);
    final VndError read = mapper.readValue(json, VndError.class);
    assertThat(read, is(original));
  }

  @Test(timeout = 1_000L)
  public void do_not_fail_on_circular_causal_chain() throws Exception {
    final RuntimeException top = new RuntimeException("top");
    final RuntimeException circular = new RuntimeException("circular");
    top.initCause(circular);
    circular.initCause(top);
    VndError.from(top, Correlation.valueOf("none"), -1L, true);
  }
}

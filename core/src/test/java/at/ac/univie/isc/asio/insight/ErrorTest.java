package at.ac.univie.isc.asio.insight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.testing.ClassSanityTester;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

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
    final List<StackTraceElement> traceList = Arrays.asList(Thread.currentThread().getStackTrace());
    final Error original = Error.create("message", "cause", correlation, 1337, traceList);
    final ObjectMapper mapper = new ObjectMapper();
    final String json = mapper.writeValueAsString(original);
    final Error read = mapper.readValue(json, Error.class);
    assertThat(read, is(original));
  }
}

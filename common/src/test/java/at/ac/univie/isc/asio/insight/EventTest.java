package at.ac.univie.isc.asio.insight;

import org.junit.Test;

public class EventTest {

  @Test(expected = AssertionError.class)
  public void should_fail_if_initialized_more_than_once() throws Exception {
    final Event event = new Event("type", "subject") {};
    event.init(Correlation.valueOf("test"), 0);
    event.init(Correlation.valueOf("test"), 1);
  }
}

package at.ac.univie.isc.asio.config;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertThat;

public class TimeoutSpecTest {

  private TimeoutSpec subject;

  @Test
  public void should_store_as_nanos() throws Exception {
    subject = TimeoutSpec.from(100, TimeUnit.SECONDS);
    assertThat(subject.value(), is(TimeUnit.SECONDS.toNanos(100)));
  }

  @Test
  public void should_convert_stored_to_given_unit() throws Exception {
    subject = TimeoutSpec.from(1, TimeUnit.DAYS);
    assertThat(subject.getAs(TimeUnit.HOURS), is(24L));
  }

  @Test
  public void undefined_is_always_negative() throws Exception {
    subject = TimeoutSpec.undefined();
    assertThat(subject.getAs(TimeUnit.DAYS), is(lessThan(0L)));
  }

  @Test
  public void negative_timeout_value_yields_UNDEFINED() throws Exception {
    subject = TimeoutSpec.from(-1, TimeUnit.SECONDS);
    assertThat(subject, is(TimeoutSpec.UNDEFINED));
  }
}

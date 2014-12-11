package at.ac.univie.isc.asio.tool;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
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
    assertThat(subject.getAs(TimeUnit.HOURS, -1L), is(24L));
  }

  @Test
  public void undefined_yields_fallback() throws Exception {
    subject = TimeoutSpec.undefined();
    assertThat(subject.getAs(TimeUnit.DAYS, 100L), is(100L));
  }

  @Test
  public void negative_timeout_value_yields_undefined() throws Exception {
    subject = TimeoutSpec.from(-1, TimeUnit.SECONDS);
    assertThat(subject.isDefined(), is(false));
  }

  @Test
  public void positive_timeout_yields_defined() throws Exception {
    subject = TimeoutSpec.from(100L, TimeUnit.SECONDS);
    assertThat(subject.isDefined(), is(true));
  }

  @Test
  public void undefined_timeout_transforms_into_default() {
    subject = TimeoutSpec.undefined();
    final TimeoutSpec alternative = TimeoutSpec.from(10, TimeUnit.SECONDS);
    assertThat(subject.orIfUndefined(alternative), is(alternative));
  }

  @Test
  public void defined_timeout_remains_as_is() {
    subject = TimeoutSpec.from(20, TimeUnit.DAYS);
    final TimeoutSpec alternative = TimeoutSpec.from(10, TimeUnit.SECONDS);
    assertThat(subject.orIfUndefined(alternative), is(subject));
  }
}

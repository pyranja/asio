package at.ac.univie.isc.asio.tool;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
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

  @Test
  public void should_roundtrip_undefined_as_string() throws Exception {
    final TimeoutSpec parsed = TimeoutSpec.fromString(TimeoutSpec.undefined().toString());
    assertThat(parsed, sameInstance(TimeoutSpec.undefined()));
  }

  @Test
  public void should_roundtrip_defined_millisecond_timeout_as_string() throws Exception {
    final TimeoutSpec original = TimeoutSpec.from(100, TimeUnit.MILLISECONDS);
    final TimeoutSpec parsed = TimeoutSpec.fromString(original.toString());
    assertThat(parsed, equalTo(original));
  }

  @Test
  public void should_lossy_roundtrip_defined_nanosecond_timeout_as_string() throws Exception {
    final TimeoutSpec original = TimeoutSpec.from(100, TimeUnit.NANOSECONDS);
    final TimeoutSpec parsed = TimeoutSpec.fromString(original.toString());
    assertThat(parsed, equalTo(TimeoutSpec.from(0, TimeUnit.MILLISECONDS)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_on_illegal_syntax() throws Exception {
    TimeoutSpec.fromString("test");
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_on_illegal_value() throws Exception {
    TimeoutSpec.fromString("ms");
  }

  @Test
  public void should_yield_undefined_for_negative_value() throws Exception {
    assertThat(TimeoutSpec.fromString("-1ms"), equalTo(TimeoutSpec.undefined()));
  }

  @Test(expected = NullPointerException.class)
  public void should_fail_on_null_input() throws Exception {
    TimeoutSpec.fromString(null);
  }
}

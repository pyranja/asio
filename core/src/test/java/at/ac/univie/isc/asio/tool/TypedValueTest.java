package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.io.Payload;
import com.google.common.base.Charsets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.annotation.Nonnull;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class TypedValueTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void should_be_a_transparent_wrapper() throws Exception {
    final String value = new String(Payload.randomWithLength(256), Charsets.UTF_8);
    final TypedValue<String> subject = new TypedValue<>(value);
    assertThat(subject.toString(), is(value));
    final TypedValue<String> other = new TypedValue<>(subject.toString());
    assertThat(other, is(equalTo(subject)));
  }

  @Test
  public void should_reject_null_value() throws Exception {
    exception.expect(NullPointerException.class);
    new TypedValue<>(null);
  }

  @Test
  public void subclasses_cannot_be_equal() throws Exception {
    final String value = "test";
    final TypedValue<String> subject = new TypedValue<String>(value) {};
    final TypedValue<String> other = new TypedValue<String>(value) {};
    assertThat(subject, is(not(equalTo(other))));
    assertThat(subject.hashCode(), is(not(equalTo(other.hashCode()))));
  }

  @Test
  public void should_call_normalize() throws Exception {
    final String replacement = "replaced";
    final TypedValue<String> subject = new TypedValue<String>("test") {
      @Nonnull
      @Override
      protected String normalize(@Nonnull final String val) {
        return replacement;
      }
    };
    assertThat(subject.toString(), is(replacement));
  }
}

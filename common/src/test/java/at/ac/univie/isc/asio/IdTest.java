package at.ac.univie.isc.asio;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class IdTest {
  @Rule
  public final ExpectedException error = ExpectedException.none();

  @Test
  public void should_reject_empty_id() throws Exception {
    error.expect(IllegalArgumentException.class);
    Id.valueOf("");
  }

  @Test
  public void should_reject_null() throws Exception {
    error.expect(NullPointerException.class);
    Id.valueOf(null);
  }

  @Test
  public void should_reject_leading_hyphen() throws Exception {
    error.expect(IllegalArgumentException.class);
    Id.valueOf("-test");
  }

  @Test
  public void should_reject_trailing_hyphen() throws Exception {
    error.expect(IllegalArgumentException.class);
    Id.valueOf("test-");
  }

  @Test
  public void should_accept_simple_identifier() throws Exception {
    assertThat(Id.valueOf("test").asString(), equalTo("test"));
  }

  @Test
  public void should_convert_to_lower_case() throws Exception {
    assertThat(Id.valueOf("TeSt").asString(), equalTo("test"));
  }

  @Test
  public void should_accept_identifier_with_enclosed_hyphen() throws Exception {
    Id.valueOf("te-st");
  }

  @Test
  public void should_accept_identifier_with_leading_underscore() throws Exception {
    Id.valueOf("_test");
  }

  @Test
  public void should_accept_identifier_with_trailing_underscore() throws Exception {
    Id.valueOf("test_");
  }

  @Test
  public void should_accept_underscore_as_identifier() throws Exception {
    Id.valueOf("_");
  }

  @Test
  public void should_reject_special_characters() throws Exception {
    error.expect(IllegalArgumentException.class);
    Id.valueOf("te/st");
  }

  @Test
  public void should_accept_numeric_identifier() throws Exception {
    assertThat(Id.valueOf("1234").asString(), equalTo("1234"));
  }
}

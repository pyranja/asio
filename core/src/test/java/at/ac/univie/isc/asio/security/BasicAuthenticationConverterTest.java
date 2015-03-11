package at.ac.univie.isc.asio.security;

import com.google.common.base.Charsets;
import com.google.common.base.Converter;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.google.common.io.BaseEncoding.base64;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class BasicAuthenticationConverterTest {
  public static class IdentityToString {
    @Rule
    public ExpectedException error = ExpectedException.none();

    private final Converter<Identity, String> subject = new BasicAuthIdentityExtractor().reverse();
    private String output;

    @Test
    public void should_write_basic_auth_scheme() throws Exception {
      output = subject.convert(Identity.from("username", "password"));
      assertThat(output, startsWith("Basic "));
    }

    @Test
    public void should_write_payload_after_basic_prefix() throws Exception {
      output = subject.convert(Identity.from("", ""));
      final String payload = output.substring("Basic ".length());
      assertThat(payload, not(isEmptyString()));
    }

    @Test
    public void should_write_colon_separator() throws Exception {
      output = subject.convert(Identity.from("", ""));
      assertThat(decoded(output), is(":"));
    }

    @Test
    public void should_write_username_as_first_part_of_payload() throws Exception {
      output = subject.convert(Identity.from("username", "password"));
      assertThat(decoded(output), startsWith("username:"));
    }

    @Test
    public void should_write_password_as_second_part_of_payload() throws Exception {
      output = subject.convert(Identity.from("username", "password"));
      assertThat(decoded(output), endsWith(":password"));
    }

    @Test
    public void should_write_full_identity() throws Exception {
      output = subject.convert(Identity.from("username", "password"));
      assertThat(decoded(output), is("username:password"));
    }

    @Test
    public void should_be_null_transparent() throws Exception {
      assertThat(subject.convert(null), is(nullValue()));
    }

    @Test
    public void should_fail_on_undefined_input() throws Exception {
      error.expect(Identity.UndefinedIdentity.class);
      subject.convert(Identity.undefined());
    }

    @Test
    public void should_accept_password_with_colon() throws Exception {
      output = subject.convert(Identity.from("username", "with:colon"));
      assertThat(decoded(output), is("username:with:colon"));
    }

    @Test
    public void should_reject_username_with_colon() throws Exception {
      error.expect(BasicAuthIdentityExtractor.MalformedCredentials.class);
      subject.convert(Identity.from("with:colon", "password"));
    }

    @Test
    public void should_reject_unnatural_large_input() throws Exception {
      error.handleAssertionErrors().expect(AssertionError.class);
      subject.convert(Identity.from("", Strings.repeat("d", 16 * 1_024)));
    }

    private String decoded(final String raw) {
      final byte[] bytes = BaseEncoding.base64().decode(raw.substring("Basic ".length()));
      return new String(bytes, Charsets.UTF_8);
    }
  }

  public static class StringToIdentity {
    @Rule
    public ExpectedException error = ExpectedException.none();

    private final Converter<String, Identity> subject = new BasicAuthIdentityExtractor();

    @Test
    public void should_auth_with_given_name_and_password() throws Exception {
      final Identity identity = subject.convert("Basic " + encoded("test-identity:test-password"));
      assertThat(identity, is(Identity.from("test-identity", "test-password")));
    }

    @Test
    public void should_auth_with_empty_name() throws Exception {
      final Identity identity = subject.convert("Basic " + encoded(":test-password"));
      assertThat(identity, is(Identity.from("", "test-password")));
    }

    @Test
    public void should_auth_with_empty_password() throws Exception {
      final Identity identity = subject.convert("Basic " + encoded("test-identity:"));
      assertThat(identity, is(Identity.from("test-identity", "")));
    }

    @Test
    public void should_auth_with_empty_name_and_secret_if_missing_in_header() throws Exception {
      final Identity identity = subject.convert("Basic " + encoded(":"));
      assertThat(identity, is(Identity.from("", "")));
    }

    @Test
    public void should_split_username_and_password_on_first_colon() throws Exception {
      final Identity identity = subject.convert("Basic " + encoded("username:password:with:colon"));
      assertThat(identity, is(Identity.from("username", "password:with:colon")));
    }

    private String encoded(final String usernameAndPassword) {
      return base64().encode(usernameAndPassword.getBytes(Charsets.UTF_8));
    }

    @Test
    public void should_be_null_transparent() throws Exception {
      final Identity identity = subject.convert(null);
      assertThat(identity, is(nullValue()));
    }

    @Test
    public void how_to_convert_null_safe() throws Exception {
      final Identity identity = Objects.firstNonNull(subject.convert(null), Identity.undefined());
      assertThat(identity, is(Identity.undefined()));
    }

    @Test
    public void should_fail_on_empty_input() throws Exception {
      error.expect(BasicAuthIdentityExtractor.MalformedCredentials.class);
      assertThat(subject.convert(""), is(Identity.undefined()));
    }

    @Test
    public void should_fail_on_non_basic_auth() throws Exception {
      error.expect(BasicAuthIdentityExtractor.MalformedCredentials.class);
      subject.convert("NONBASIC ABC");
    }

    @Test
    public void should_fail_when_no_encoded_credentials_given() throws Exception {
      error.expect(BasicAuthIdentityExtractor.MalformedCredentials.class);
      subject.convert("Basic ");
    }

    @Test
    public void should_fail_on_malformed_credentials() throws Exception {
      error.expect(BasicAuthIdentityExtractor.MalformedCredentials.class);
      final String malformed = encoded("no_colon");
      subject.convert("Basic " + malformed);
    }
  }
}

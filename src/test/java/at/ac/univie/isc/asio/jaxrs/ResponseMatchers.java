package at.ac.univie.isc.asio.jaxrs;

import com.google.common.base.Charsets;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

public final class ResponseMatchers {
  private ResponseMatchers() { /* no instances */ }

  @Factory
  public static ResponseStatusMatcher hasStatus(Response.Status expected) {
    return new ResponseStatusMatcher(expected);
  }

  @Factory
  public static ResponseStatusMatcher hasFamily(final Response.Status.Family expected) {
    return new ResponseStatusMatcher(new FamilyOnlyStatusType(expected));
  }

  @Factory
  public static Matcher<MediaType> compatibleTo(final MediaType reference) {
    return new CompatibleTo(reference);
  }

  @Factory
  public static ResponseBodyMatcher hasBody(final byte[] expected) {
    return new ResponseBodyMatcher(expected);
  }

  @Factory
  public static ResponseBodyMatcher hasBody(final String expected) {
    return new ResponseBodyMatcher(expected.getBytes(Charsets.UTF_8));
  }

  // ********************************** response status

  static class ResponseStatusMatcher extends TypeSafeMatcher<Response> {
    private final Response.StatusType expected;

    private ResponseStatusMatcher(Response.StatusType expected) {
      this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(final Response item) {
      if (expected.getStatusCode() > 0) {
        return expected.getStatusCode() == item.getStatus();
      } else {
        return expected.getFamily().equals(item.getStatusInfo().getFamily());
      }
    }

    @Override
    protected void describeMismatchSafely(final Response item, final Description mismatchDescription) {
      final Response.StatusType status = item.getStatusInfo();
      mismatchDescription.appendText(formatStatus(status));
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText(formatStatus(expected));
    }

    private String formatStatus(Response.StatusType status) {
      return String.format(Locale.ENGLISH, "%2$13s : %1$s %3$s",
          status.getStatusCode(), status.getFamily(), status.getReasonPhrase());
    }
  }

  private static class FamilyOnlyStatusType implements Response.StatusType {

    private final Response.Status.Family expected;

    public FamilyOnlyStatusType(final Response.Status.Family expected) {
      this.expected = expected;
    }

    @Override
    public int getStatusCode() {
      return 0;
    }

    @Override
    public Response.Status.Family getFamily() {
      return expected;
    }

    @Override
    public String getReasonPhrase() {
      return "not specified";
    }
  }

  // ********************************** media type

  public static class CompatibleTo extends TypeSafeMatcher<MediaType> {

    private final MediaType reference;

    public CompatibleTo(final MediaType reference) {
      this.reference = requireNonNull(reference);
    }

    @Override
    protected boolean matchesSafely(final MediaType item) {
      return reference.isCompatible(item) || suffixExtensionMatches(item);
    }

    private boolean suffixExtensionMatches(final MediaType item) {
      // check for extended MIME syntax as defined in RFC 3023 : "type/*+subtype"
      final String suffixRegex = String.format(Locale.ENGLISH, ".+\\+%s", reference.getSubtype());
      return reference.getType().equalsIgnoreCase(item.getType())  // types match
          && item.getSubtype().matches(suffixRegex);  // has reference subtype as suffix
    }

    @Override
    public void describeTo(final Description description) {
      description.appendValue(reference);
    }
  }

  // ********************************** body

  public static class ResponseBodyMatcher extends TypeSafeMatcher<Response> {
    private final byte[] expected;

    public ResponseBodyMatcher(final byte[] expected) {
      this.expected = requireNonNull(expected);
    }

    @Override
    protected boolean matchesSafely(final Response response) {
      response.bufferEntity();
      return Arrays.equals(expected, response.readEntity(byte[].class));
    }

    @Override
    protected void describeMismatchSafely(final Response item, final Description mismatchDescription) {
      mismatchDescription.appendValue(item.readEntity(String.class));
    }

    @Override
    public void describeTo(final Description description) {
      description.appendValue(new String(expected, Charsets.UTF_8));
    }
  }
}

package at.ac.univie.isc.asio.jaxrs;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

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

  @Factory
  public static ResponseHeaderMatcher hasHeaders(final Matcher<String> keyMatcher, final Matcher<? super List<String>> valueMatcher) {
    return new ResponseHeaderMatcher(keyMatcher, valueMatcher);
  }

  @Factory
  public static ResponseHeaderMatcher hasHeader(final Matcher<String> keyMatcher, final Matcher<String> valueMatcher) {
    return new ResponseHeaderMatcher(keyMatcher, hasItem(valueMatcher));
  }

  @Factory
  public static ResponseHeaderMatcher hasHeader(final String key, final String... values) {
    return new ResponseHeaderMatcher(equalToIgnoringCase(key), containsInAnyOrder(values));
  }

  @Factory
  public static ResponseHeaderMatcher hasHeader(final String key, final Matcher<String> valueMatcher) {
    return new ResponseHeaderMatcher(equalToIgnoringCase(key), hasItem(valueMatcher));
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

  // ********************************** headers

  public static class ResponseHeaderMatcher extends TypeSafeMatcher<Response> {
    private final Matcher<String> keyMatcher;
    private final Matcher<? super List<String>> valueMatcher;

    private ResponseHeaderMatcher(final Matcher<String> keyMatcher, final Matcher<? super List<String>> valueMatcher) {
      this.keyMatcher = keyMatcher;
      this.valueMatcher = valueMatcher;
    }

    @Override
    protected boolean matchesSafely(final Response response) {
      final MultivaluedMap<String, Object> rawHeaders = response.getHeaders();
      final Map<String, List<String>> headers =
          Maps.transformValues(rawHeaders, new Function<List<Object>, List<String>>() {
            @Override
            public List<String> apply(final List<Object> input) {
              return Lists.transform(input, Functions.toStringFunction());
            }
          });
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        if (keyMatcher.matches(entry.getKey()) && valueMatcher.matches(entry.getValue())) {
          return true;
        }
      }
      return false;
    }

    @Override
    public void describeTo(final Description description) {
      description
          .appendText("headers containing [")
          .appendDescriptionOf(keyMatcher)
          .appendText("->")
          .appendDescriptionOf(valueMatcher)
          .appendText("]");
    }

    @Override
    protected void describeMismatchSafely(final Response item, final Description mismatchDescription) {
      mismatchDescription.appendText("headers were ").appendValue(item.getHeaders());
    }
  }
}

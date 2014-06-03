package at.ac.univie.isc.asio.tool;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import javax.ws.rs.core.Response;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA. User: borck_000 ; Date: 6/3/2014 ; Time: 1:02 PM
 */
public final class JaxrsResponseMatchers {
  private JaxrsResponseMatchers() { /* no instances */ }

  @Factory
  public static ResponseStatusMatcher hasStatus(Response.Status expected) {
    return new ResponseStatusMatcher(expected);
  }

  @Factory
  public static ResponseStatusMatcher hasFamily(final Response.Status.Family expected) {
    return new ResponseStatusMatcher(new FamilyOnlyStatusType(expected));
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
}

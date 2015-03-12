package at.ac.univie.isc.asio.security.uri;

import at.ac.univie.isc.asio.security.FindAuthorization;
import at.ac.univie.isc.asio.security.Role;
import org.hamcrest.Matchers;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

@RunWith(Theories.class)
public class StaticRedirectTest {
  @DataPoint
  public static String root = "/";
  @DataPoint
  public static String singleElement = "/element";
  @DataPoint
  public static String empty = "";
  
  @Theory
  public void should_not_accept_match_if_marker_not_present(final String marker) throws Exception {
    assumeThat("not_the_marker", not(startsWith(marker)));
    final boolean accepted = StaticRedirect.create(marker, "ignored").canHandle(path("not_the_marker"));
    assertThat("marker was " + marker, accepted, is(false));
  }

  @Theory
  public void should_not_accept_match_if_marker_present_bot_not_prefix_of_tail(final String marker) throws Exception {
    assumeThat("not_the_marker", not(startsWith(marker)));
    final boolean accepted = StaticRedirect.create(marker, "ignored").canHandle(path(
        "not_the_marker" + marker));
    assertThat("marker was " + marker, accepted, is(false));
  }

  @Theory
  public void should_handle_match_where_marker_is_the_tail(final String marker) throws Exception {
    final boolean accepted = StaticRedirect.create(marker, "ignored").canHandle(path(marker));
    assertThat(accepted, is(true));
  }

  @Theory
  public void should_handle_match_where_marker_is_prefix_of_the_tail(final String marker) throws Exception {
    final boolean accepted = StaticRedirect.create(marker, "ignored").canHandle(path(
        marker + "/tail"));
    assertThat(accepted, is(true));
  }

  @Theory
  public void should_set_authority_to__none__() throws Exception {
    final FindAuthorization.AuthAndRedirect redirect = StaticRedirect.create("/marker", "ignored")
        .handle(path("/marker"));
    assertThat(redirect.authority().getAuthority(), Matchers.is(Role.NONE.name()));
  }

  @Theory
  public void should_append_match_tail_to_redirect(final String base) throws Exception {
    final FindAuthorization.AuthAndRedirect redirect = StaticRedirect.create("/marker", base)
        .handle(path("/marker/rest/of/path"));
    assertThat(redirect.redirection().get(), is(base + "/rest/of/path"));
  }

  private UriAuthRule.PathElements.Mock path(final String tail) {
    return UriAuthRule.PathElements.Mock.from(Collections.singletonMap("tail", tail));
  }
}

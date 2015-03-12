package at.ac.univie.isc.asio.security.uri;

import at.ac.univie.isc.asio.security.FindAuthorization;
import at.ac.univie.isc.asio.security.Role;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test rewrite rules for vph deployment:
 *   Rewrite static content to base path with Role#NONE
 *   Extract role and rewrite to URI without role
 */
@RunWith(Parameterized.class)
public class VphUriAuthRewritingTest {
  private static final String STATIC_MARKER = "/explore";
  private static final String STATIC_BASE = "/static";

  @Parameterized.Parameters(name = "{index} : {0} -> {1}")
  public static Iterable<Object[]> uris() {
    // { uri, expected result }
    return Arrays.asList(new Object[][] {
        // catalog requests
        {"/head/admin/tail", expect("admin", "/head/tail")}
        , {"/head/admin/tail/", expect("admin", "/head/tail/")}
        , {"/head/admin/longer/tail", expect("admin", "/head/longer/tail")}
        , {"/head/admin/", expect("admin", "/head/")}
        , {"/head/admin", expect("admin", "/head")}
        , {"/head/admin/tail?key=value", expect("admin", "/head/tail?key=value")}
        , {"/head#fragment/admin/tail#fragment", expect("admin", "/head#fragment/tail#fragment")}
        , {"/head/admin?key=value", expect("admin", "/head?key=value")}
        , {"/head/admin#fragment", expect("admin", "/head#fragment")}
        // static content
        , {"/head/admin/explore/tail", expect(Role.NONE.name(), "/static/tail")}
        , {"/head/admin/explore/tail/", expect(Role.NONE.name(), "/static/tail/")}
        , {"/head/explore/explore/tail", expect(Role.NONE.name(), "/static/tail")}
        , {"/head/admin/explore/index.html", expect(Role.NONE.name(), "/static/index.html")}
        // do not get confused by prefix
        , {"/head/explore", expect("explore", "/head")}
        // illegal requests
        , {"//", expectError()}
        , {"///", expectError()}
        , {"/too-short/", expectError()}
        , {"not/absolute/", expectError()}
        , {"/head/?role", expectError()}
        , {"/head/#role", expectError()}
    });
  }

  private static FindAuthorization.AuthAndRedirect expect(final String role, final String path) {
    return FindAuthorization.AuthAndRedirect.create(new SimpleGrantedAuthority(role), path);
  }

  private static FindAuthorization.AuthAndRedirect expectError() {
    return null;
  }

  @Rule
  public ExpectedException error = ExpectedException.none();

  @Parameterized.Parameter(0)
  public String uri;
  @Parameterized.Parameter(1)
  public FindAuthorization.AuthAndRedirect expected;

  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private FindAuthorization subject = RuleBasedFinder.create(
      ExtractRole.URI_WITH_ROLE_REGEX,
      StaticRedirect.create(STATIC_MARKER, STATIC_BASE),
      ExtractRole.instance()
  );

  @Before
  public void expectErrorIfNoResultGiven() {
    if (expected == null) { error.expect(UriParser.MalformedUri.class); }
  }

  @Before
  public void initializeRequest() {
    request.setRequestURI(uri);
  }

  @Test
  public void should_extract_authority() throws Exception {
    assertThat(subject.accept(request).authority(), is(expected.authority()));
  }

  @Test
  public void should_extract_redirection() throws Exception {
    assertThat(subject.accept(request).redirection(), is(expected.redirection()));
  }

  @Test
  public void should_ignore_given_prefix() throws Exception {
    request.setRequestURI("/prefix" + uri);
    request.setContextPath("/prefix");
    assertThat(subject.accept(request), is(expected));
  }

  @Test
  public void should_parse_identical_multiple_times() throws Exception {
    for (int i = 0; i < 10; i++) {
      assertThat("fail on iteration <" + i + ">", subject.accept(request), is(expected));
    }
  }
}

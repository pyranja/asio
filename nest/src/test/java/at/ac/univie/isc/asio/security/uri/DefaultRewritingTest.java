package at.ac.univie.isc.asio.security.uri;

import at.ac.univie.isc.asio.security.FindAuthorization;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test default rewrite rules:
 *  Only rewrite static content, all else pass through unchanged
 */
@RunWith(Parameterized.class)
public class DefaultRewritingTest {
  private static final String STATIC_MARKER = "/explore";
  private static final String STATIC_BASE = "/static";

  @Parameterized.Parameters(name = "{index} : {0} -> {1}")
  public static Iterable<Object[]> uris() {
    // { uri, expected result }
    return Arrays.asList(new Object[][] {
        // catalog requests
        {"/head/tail", noRedirect()}
        , {"/head/tail/", noRedirect()}
        , {"/head/longer/tail", noRedirect()}
        , {"/head/", noRedirect()}
        , {"/head", noRedirect()}
        , {"/head/tail?key=value", noRedirect()}
        , {"/head#fragment/tail#fragment", noRedirect()}
        , {"/head?key=value", noRedirect()}
        , {"/head#fragment", noRedirect()}
        // must expect head element
        , {"/explore", noRedirect()}
        , {"/explore/tail", noRedirect()}
        // static content
        , {"/head/explore/tail", expect("/static/tail")}
        , {"/head/explore/tail/", expect("/static/tail/")}
        , {"/head/explore/explore/tail", expect("/static/explore/tail")}
        , {"/head/explore/index.html", expect("/static/index.html")}
        , {"/head/explore", expect("/static")}
        // illegal requests
        , {"//", expectError()}
        , {"///", expectError()}
        , {"not/absolute/", expectError()}
    });
  }

  private static FindAuthorization.AuthAndRedirect expect(final String path) {
    return FindAuthorization.AuthAndRedirect.create(FindAuthorization.AuthAndRedirect.NO_AUTHORITY, path);
  }

  private static FindAuthorization.AuthAndRedirect noRedirect() {
    return FindAuthorization.AuthAndRedirect.noRedirect(FindAuthorization.AuthAndRedirect.NO_AUTHORITY);
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
      StaticRedirect.URI_WITH_SCHEMA_HEAD,
      StaticRedirect.create(STATIC_MARKER, STATIC_BASE),
      NoopRule.instance()
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

package at.ac.univie.isc.asio.security;

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

@RunWith(Parameterized.class)
public class VphUriRewriterTest {
  private static final String STATIC_PATH_PREFIX = "/static";
  private static final String CATALOG_PATH_PREFIX = "/catalog";

  @Parameterized.Parameters(name = "{index} : {0} -> {1}")
  public static Iterable<Object[]> uris() {
    // { uri, expected result }
    return Arrays.asList(new Object[][] {
        // catalog requests
        { "/head/admin/tail", expect("admin", "/catalog/head/tail")}
        , { "/head/admin/tail/", expect("admin", "/catalog/head/tail/") }
        , { "/head/admin/longer/tail", expect("admin", "/catalog/head/longer/tail") }
        , { "/head/admin/", expect("admin", "/catalog/head/") }
        , { "/head/admin", expect("admin", "/catalog/head") }
        , { "/head/admin/tail?key=value", expect("admin", "/catalog/head/tail?key=value") }
        , { "/head#fragment/admin/tail#fragment", expect("admin", "/catalog/head#fragment/tail#fragment") }
        , { "/head/admin?key=value", expect("admin", "/catalog/head?key=value") }
        , { "/head/admin#fragment", expect("admin", "/catalog/head#fragment") }
        // static content
        , { "/head/admin/static/tail", expect(Role.NONE.name(), "/tail") }
        , { "/head/admin/static/tail/", expect(Role.NONE.name(), "/tail/") }
        , { "/head/static/static/tail", expect(Role.NONE.name(), "/tail") }
        , { "/head/admin/static/index.html", expect(Role.NONE.name(), "/index.html") }
        // do not get confused by prefix
        , { "/head/static", expect("static", "/catalog/head") }
        // illegal requests
        , { "//", expectError() }
        , { "///", expectError() }
        , { "/too-short/", expectError() }
        , { "not/absolute/", expectError() }
        , { "/head/?role", expectError() }
        , { "/head/#role", expectError() }
    });
  }

  private static FindAuthorization.Result expect(final String role, final String path) {
    return FindAuthorization.Result.create(new SimpleGrantedAuthority(role), path);
  }

  private static FindAuthorization.Result expectError() {
    return null;
  }

  @Rule
  public ExpectedException error = ExpectedException.none();

  @Parameterized.Parameter(0)
  public String uri;
  @Parameterized.Parameter(1)
  public FindAuthorization.Result expected;

  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private VphUriRewriter subject = VphUriRewriter.withPrefixes(CATALOG_PATH_PREFIX, STATIC_PATH_PREFIX);

  @Before
  public void expectErrorIfNoResultGiven() {
    if (expected == null) {
      error.expect(VphUriRewriter.MalformedUri.class);
    }
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
  public void when_static_path_ends_with_slash() throws Exception {
    subject = VphUriRewriter.withPrefixes(CATALOG_PATH_PREFIX, STATIC_PATH_PREFIX + "/");
    assertThat(subject.accept(request), is(expected));
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

package at.ac.univie.isc.asio.insight;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class ExplorerPageRedirectFilterTest {

  public static class DoFilter {
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();
    private final MockTemplate template = new MockTemplate();

    private final ExplorerPageRedirectFilter subject = new ExplorerPageRedirectFilter(template);

    @Test
    public void should_skip_if_no_redirect_required() throws Exception {
      template.redirect = null;
      subject.doFilterInternal(request, response, chain);
      assertThat(chain.getRequest(), Matchers.<ServletRequest>sameInstance(request));
      assertThat(chain.getResponse(), Matchers.<ServletResponse>sameInstance(response));
    }

    @Test
    public void should_forward_to_found_redirect() throws Exception {
      template.redirect = "/redirect";
      subject.doFilterInternal(request, response, chain);
      assertThat(response.getForwardedUrl(), equalTo("/redirect"));
    }
  }

  @RunWith(Parameterized.class)
  public static class FindRedirectTarget {
    @Parameterized.Parameters(name = "{index}: {0} should redirect to {1}")
    public static Iterable<Object[]> uriAndRedirect() {
      // request-path, redirect-target
      return Arrays.asList(new Object[][] {
          // skip these as path is equal to the correct redirect
          {"/marker", null},
          {"/marker/", null},
          {"/marker/file.html", null},
          // these paths require a redirect
          {"/head/marker", "/marker"},
          {"/head/marker/", "/marker/"},
          {"/head/marker/file.html", "/marker/file.html"},
          {"/head/marker/tail", "/marker/tail"},
          {"/head/marker/tail/", "/marker/tail/"},
          {"/head/marker/tail/file.html", "/marker/tail/file.html"},
          // redirect /favicon.ico requests to content root
          {"/favicon.ico", "/marker/favicon.ico"},
          {"/head/some/path/favicon.ico", null},
          {"/head/marker/tail/favicon.ico", "/marker/tail/favicon.ico"},
          // do not get fooled by encoded slashes
          {"/head%2Fmarker/tail", "/marker/tail"},
          {"/head/marker%2Ftail/", "/marker/tail/"},
          {"/head%2Fmarker%2Ftail/file.html", "/marker/tail/file.html"},
          // match marker case insensitive but keep casing in redirect
          {"/HeAd/MaRkEr", "/MaRkEr"},
          {"/HeAd/MaRkEr/", "/MaRkEr/"},
          {"/HeAd/MaRkEr/FiLe.HtMl", "/MaRkEr/FiLe.HtMl"},
          // do not get fooled by element with marker substring
          {"/head/markerme/file.html", null},
          {"/head/marker.js", null},
          // should redirect from first occurrence of marker
          {"/head/marker/marker/file.html", "/marker/marker/file.html"},
          // skip redirecting where marker missing
          {"/", null},
          {"//", null},
          {"///", null},
          {"/no-marker", null},
          {"/no-marker/", null},
          {"/no-marker/file.html", null},
      });
    }

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final ExplorerPageRedirectFilter.ExplorerTemplate subject =
        new ExplorerPageRedirectFilter.ExplorerTemplate("/marker");

    @Parameterized.Parameter(0)
    public String path;
    @Parameterized.Parameter(1)
    public String expected;

    @Test
    public void should_redirect_to_expected_target() throws Exception {
      request.setRequestURI(path);
      assertThat(subject.findRedirectTarget(request), equalTo(expected));
    }

    @Test
    public void should_ignore_context_path() throws Exception {
      request.setContextPath("/context");
      request.setRequestURI("/context" + path);
      assertThat(subject.findRedirectTarget(request), equalTo(expected));
    }
  }

  private static class MockTemplate extends ExplorerPageRedirectFilter.ExplorerTemplate {
    public String redirect;

    public MockTemplate() {
      super("none");
    }

    @Override
    String findRedirectTarget(final HttpServletRequest request) {
      return redirect;
    }
  }
}

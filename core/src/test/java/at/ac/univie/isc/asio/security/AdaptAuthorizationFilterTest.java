package at.ac.univie.isc.asio.security;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

public class AdaptAuthorizationFilterTest {
  private final FindAuthorization authorizer = Mockito.mock(FindAuthorization.class);
  private final TranslateAuthorization adapter = Mockito.mock(TranslateAuthorization.class);

  private final AdaptAuthorizationFilter subject = new AdaptAuthorizationFilter(authorizer, adapter);

  private final DispatcherInjectableRequest request = new DispatcherInjectableRequest();
  private final MockHttpServletResponse response = new MockHttpServletResponse();
  private final MockFilterChain chain = new MockFilterChain();

  @Before
  public void prepare_servlet_mocks() throws Exception {
    request.setRequestURI("/context/original/path");
    request.setContextPath("/context");
  }

  @Test
  public void successful_authorization() throws Exception {
    given(authorizer.accept(request))
        .willReturn(FindAuthorization.Result.create(Role.ADMIN, null));
    given(adapter.translate(Role.ADMIN, request, response))
        .willReturn(TranslateAuthorization.Wrapped.create(request, response));
    subject.doFilter(request, response, chain);
    assertThat(chain.getRequest(), Matchers.<ServletRequest>sameInstance(request));
    assertThat(chain.getResponse(), Matchers.<ServletResponse>sameInstance(response));
  }

  @Test
  public void successful_authorization_with_forward() throws Exception {
    given(authorizer.accept(request))
        .willReturn(FindAuthorization.Result.create(Role.ADMIN, "/redirection/path"));
    given(adapter.translate(Role.ADMIN, request, response))
        .willReturn(TranslateAuthorization.Wrapped.create(request, response));
    subject.doFilter(request, response, chain);
    assertThat(response.getForwardedUrl(), is("/redirection/path"));
  }

  @Test
  public void invalid_request_uri() throws Exception {
    given(authorizer.accept(request))
        .willThrow(new VphUriRewriter.MalformedUri("test"));
    subject.doFilter(request, response, chain);
    assertThat(response.getErrorMessage(), is("cannot extract authorization from request uri <test>"));
    assertThat(response.getStatus(), is(HttpStatus.SC_UNAUTHORIZED));
  }

  @Test
  public void authentication_error() throws Exception {
    given(authorizer.accept(request))
        .willReturn(FindAuthorization.Result.create(Role.ADMIN, "/redirection/path"));
    given(adapter.translate(Role.ADMIN, request, response))
        .willThrow(new BasicAuthConverter.MalformedCredentials("test"));
    subject.doFilter(request, response, chain);
    assertThat(response.getErrorMessage(), is("test"));
    assertThat(response.getStatus(), is(HttpStatus.SC_UNAUTHORIZED));
  }

  @Test
  public void no_dispatcher_found() throws Exception {
    given(authorizer.accept(request))
        .willReturn(FindAuthorization.Result.create(Role.ADMIN, "/redirection/path"));
    given(adapter.translate(Role.ADMIN, request, response))
        .willReturn(TranslateAuthorization.Wrapped.create(request, response));
    request.setForceIllegalRedirect(true);
    subject.doFilter(request, response, chain);
    assertThat(response.getErrorMessage(), is("no handler for request to </redirection/path> (redirected from </context/original/path>) found"));
    assertThat(response.getStatus(), is(HttpStatus.SC_NOT_FOUND));
  }

  /** allows to simulate illegal redirection targets */
  private static class DispatcherInjectableRequest extends MockHttpServletRequest {
    private boolean forceIllegalRedirect = false;

    public void setForceIllegalRedirect(final boolean forceIllegalRedirect) {
      this.forceIllegalRedirect = forceIllegalRedirect;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(final String path) {
      return forceIllegalRedirect ? null : super.getRequestDispatcher(path);
    }
  }
}

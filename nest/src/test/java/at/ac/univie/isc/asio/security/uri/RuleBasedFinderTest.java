package at.ac.univie.isc.asio.security.uri;

import at.ac.univie.isc.asio.security.uri.UriAuthRule;
import at.ac.univie.isc.asio.security.uri.NoopRule;
import at.ac.univie.isc.asio.security.uri.RuleBasedFinder;
import at.ac.univie.isc.asio.security.uri.UriParser;
import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.AuthenticationException;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class RuleBasedFinderTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final UriParser parser = Mockito.mock(UriParser.class);
  private final UriAuthRule first = Mockito.mock(UriAuthRule.class);
  private final UriAuthRule second = Mockito.mock(UriAuthRule.class);
  private final List<UriAuthRule> rules = ImmutableList.of(first, second, NoopRule.instance());
  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private final RuleBasedFinder subject = new RuleBasedFinder(parser, rules);

  @Test
  public void should_forward_request_params_to_parser() throws Exception {
    request.setRequestURI("uri");
    request.setContextPath("context");
    subject.accept(request);
    verify(parser).parse("uri", "context");
  }

  @Test
  public void should_forward_parser_failure() throws Exception {
    final IllegalStateException failure = new IllegalStateException("test");
    when(parser.parse(anyString(), anyString())).thenThrow(failure);
    error.expect(sameInstance(failure));
    subject.accept(request);
  }

  @Test
  public void should_invoke_only_first_matching_rule() throws Exception {
    when(first.canHandle(any(UriAuthRule.PathElements.class))).thenReturn(true);
    when(second.canHandle(any(UriAuthRule.PathElements.class))).thenReturn(true);
    subject.accept(request);
    verify(first).handle(null);
    verifyZeroInteractions(second);
  }

  @Test
  public void should_ask_all_rules_for_match() throws Exception {
    subject.accept(request);
    verify(first).canHandle(null);
    verify(second).canHandle(null);
  }

  @Test
  public void should_fail_if_no_rule_matches() throws Exception {
    error.expect(AuthenticationException.class);
    new RuleBasedFinder(parser, Collections.<UriAuthRule>emptyList()).accept(request);
  }
}

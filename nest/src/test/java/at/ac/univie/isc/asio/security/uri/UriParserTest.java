package at.ac.univie.isc.asio.security.uri;

import at.ac.univie.isc.asio.security.uri.UriAuthRule;
import at.ac.univie.isc.asio.security.uri.UriParser;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class UriParserTest {
  @DataPoint
  public static String noContext = null;
  @DataPoint
  public static String simpleContext = "/context";
  @DataPoint
  public static String regexContext = "[test]";
  @DataPoint
  public static String contextWithRegexBounds = "^.*$";

  @Rule
  public ExpectedException error = ExpectedException.none();

  private UriAuthRule.PathElements result;

  @Theory
  public void should_match_any_to_wildcard_regex(final String context) throws Exception {
    result = UriParser.create(".*").parse(concat(context, "/uri"), context);
    assertThat(result, is(not(nullValue())));
  }

  @Theory
  public void should_fail_on_non_matching_uri(final String context) throws Exception {
    error.expect(UriParser.MalformedUri.class);
    UriParser.create("/test").parse(concat(context, "/nottest"), context);
  }

  @Theory
  public void should_have_matching_groups_in_returned_match(final String context) throws Exception {
    result = UriParser.create("/(?<group>.*)").parse(concat(context, "/text"), context);
    assertThat(result.require("group"), is("text"));
  }

  @Theory
  public void should_fail_if_requested_group_does_not_exist(final String context) throws Exception {
    result = UriParser.create("/(?<group>.*)").parse(concat(context, "/text"), context);
    error.expect(IllegalArgumentException.class);
    result.require("not-there");
  }

  @Theory
  public void should_fail_if_requested_group_was_not_matched(final String context) throws Exception {
    result =
        UriParser.create("/text/(?<group>will-not-match)?").parse(concat(context, "/text/"), context);
    error.expect(IllegalArgumentException.class);
    result.require("group");
  }

  @Theory
  public void should_normalize_matched_groups_to_lower_case() throws Exception {
    result = UriParser.create("/(?<group>.*)").parse("/TeXt", null);
    assertThat(result.require("group"), is("text"));
  }

  @Theory
  public void should_reject_null_uri() {
    error.expect(UriParser.MalformedUri.class);
    UriParser.create("/test").parse(null, "/test");
  }

  private String concat(final String context, final String uri) {
    return context == null
        ? "" + uri
        : context + uri;
  }
}

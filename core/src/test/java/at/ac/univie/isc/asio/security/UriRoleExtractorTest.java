package at.ac.univie.isc.asio.security;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UriRoleExtractorTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private UriPermissionExtractor subject;
  private UriPermissionExtractor.Result result;

  @Before
  public void setUp() throws Exception {
    subject = new UriPermissionExtractor();
  }

  @Test
  public void extract_permission_from_valid_uri() throws Exception {
    result = subject.accept("/permission/tail", "");
    assertThat(result.permission(), is("permission"));
  }

  @Test
  public void extract_tail_from_valid_uri() throws Exception {
    result = subject.accept("/permission/tail", "");
    assertThat(result.tail(), is("/tail"));
  }

  @Test
  public void tail_from_long_uri() throws Exception {
    result = subject.accept("/permission/longer/tail", "");
    assertThat(result.tail(), is("/longer/tail"));
  }

  @Test
  public void empty_tail_yields_root_path() throws Exception {
    result = subject.accept("/permission", "");
    assertThat(result.tail(), is("/"));
  }

  @Test
  public void reject_empty() throws Exception {
    error.expect(UriPermissionExtractor.MalformedUri.class);
    subject.accept("", "");
  }

  @Test
  public void reject_relative_uri() throws Exception {
    error.expect(UriPermissionExtractor.MalformedUri.class);
    subject.accept("permission/tail", "");
  }

  @Test
  public void reject_empty_permission() throws Exception {
    error.expect(UriPermissionExtractor.MalformedUri.class);
    subject.accept("//", "");
  }

  @Test
  public void strips_given_context() throws Exception {
    result = subject.accept("/prefix/permission/tail", "/prefix");
    assertThat(result, is(UriPermissionExtractor.result("permission", "/tail")));
  }

  @Test
  public void prefix_stripping_is_case_insensitive() throws Exception {
    result = subject.accept("/PrEfIx/permission/tail", "/prefix");
    assertThat(result, is(UriPermissionExtractor.result("permission", "/tail")));
  }

  @Test
  public void prevent_prefix_regex_injection() throws Exception {
    error.expect(UriPermissionExtractor.MalformedUri.class);
    subject.accept("/illegal/permission/tail", ".?");
  }

  @Test
  public void null_prefix_is_handled_as_empty() throws Exception {
    result = subject.accept("/permission/tail", null);
    assertThat(result, is(UriPermissionExtractor.result("permission", "/tail")));
  }

  @Test
  public void reject_null_uri() throws Exception {
    error.expect(UriPermissionExtractor.MalformedUri.class);
    subject.accept(null, "");
  }
}

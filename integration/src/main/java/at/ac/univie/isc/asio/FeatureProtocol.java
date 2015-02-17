package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.web.HttpCode;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URLEncoder;
import java.util.Arrays;

import static at.ac.univie.isc.asio.matcher.AsioMatchers.compatibleTo;
import static at.ac.univie.isc.asio.web.HttpMatchers.indicates;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assume.assumeThat;

/**
 * Verify compliance of an endpoint with the generalized protocol specification.
 */
@Category(Integration.class)
@RunWith(Parameterized.class)
public class FeatureProtocol extends IntegrationTest {

  @Parameterized.Parameters(name = "{index} : {0}-{1}")
  public static Iterable<Object[]> variants() {
    // { language, operation, noop_command, required_permission }
    return Arrays.asList(new Object[][] {
        {"sql", "query", "SELECT 1", "read"},
        {"sql", "update", "DROP TABLE IF EXISTS test_gaga_12345", "full"},
        {"sparql", "query", "ASK {}", "read"},
    });
  }

  @Parameterized.Parameter(0)
  public String language;
  @Parameterized.Parameter(1)
  public String operation;
  @Parameterized.Parameter(2)
  public String noop;
  @Parameterized.Parameter(3)
  public String permission;

  private void ensureReadOnly() {
    assumeThat("modifying operations not allowed via GET", operation, is("query"));
  }

  @Before
  public void ensureEndpointForLanguageExists() {
    ensureLanguageSupported(language);
  }

  // @formatter:off

  // === valid operations ==========================================================================

  @Test
  public void valid_via_http_GET() throws Exception {
    ensureReadOnly();
    givenPermission(permission)
      .param(operation,  noop)
    .when()
      .get("/{language}", language)
    .then()
      .statusCode(indicates(HttpCode.SUCCESSFUL));
  }

  @Test
  public void valid_via_http_form_submission() throws Exception {
    givenPermission(permission)
      .param(operation, noop)
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(indicates(HttpCode.SUCCESSFUL));
  }

  @Test
  public void valid_via_http_POST_with_raw_payload() throws Exception {
    final String operationContentType = Pretty.format("application/%s-%s", language, operation);
    givenPermission(permission)
      .body(Payload.encodeUtf8(noop))
      .contentType(operationContentType)
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(indicates(HttpCode.SUCCESSFUL));
  }


  // === content negotiation =======================================================================

  @Test
  public void support_xml_response_format() throws Exception {
    givenPermission(permission)
      .param(operation, noop)
      .header(HttpHeaders.ACCEPT, "application/xml")
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(indicates(HttpCode.SUCCESSFUL))
      .contentType(compatibleTo("application/xml"));
  }

  @Test
  public void support_json_response_format() throws Exception {
    // TODO : implement json formats
    assumeThat("sql json formats not implemented", language, is(not("sql")));
    givenPermission(permission)
      .param(operation, noop)
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(indicates(HttpCode.SUCCESSFUL))
      .contentType(compatibleTo("application/json"));
  }

  @Test
  public void support_csv_response_format() throws Exception {
    givenPermission(permission)
      .param(operation, noop)
      .header(HttpHeaders.ACCEPT, "text/csv")
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(indicates(HttpCode.SUCCESSFUL))
      .contentType(compatibleTo("text/csv"));
  }

  @Test
  public void defaults_to_xml_content_if_no_accept_header_given() throws Exception {
    givenPermission(permission)
      .param(operation, noop)
    .when()
      .post("/{language}", language)
    .then()
      .contentType(compatibleTo("application/xml"));
  }

  @Test
  public void defaults_to_xml_content_if_wildcard_accept_header_given() throws Exception {
    givenPermission(permission)
      .header(HttpHeaders.ACCEPT, "*/*")
      .param(operation, noop)
    .when()
      .post("/{language}", language)
    .then()
      .contentType(compatibleTo("application/xml"));
  }

  @Test
  public void override_accepted_header_using_asio_query_parameter() throws Exception {
    givenPermission(permission)
      .header(HttpHeaders.ACCEPT, "application/json")
      .param(operation, noop)
      .queryParam("x-asio-accept", "text/csv")
    .when()
      .post("/{language}", language)
    .then()
      .contentType(compatibleTo("text/csv"));
  }

  @Test
  public void override_accepted_header_using_cxf_query_parameter() throws Exception {
    givenPermission(permission)
      .header(HttpHeaders.ACCEPT, "application/json")
      .param(operation, noop)
      .queryParam("_type", "text/csv")
    .when()
      .post("/{language}", language)
    .then()
      .contentType(compatibleTo("text/csv"));
  }

  @Test
  public void reject_unacceptable_media_type() throws Exception {
    givenPermission(permission)
      .header(HttpHeaders.ACCEPT, "image/jpeg")
      .param(operation, noop)
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(is(HttpStatus.SC_NOT_ACCEPTABLE));
  }

  // === invalid operations ========================================================================

  @Test
  public void reject_unsupported_language() throws Exception {
    givenPermission(permission)
      .param(operation, noop)
    .when()
      .get("/{permission}/unknown-language", permission)
    .then()
      .statusCode(is(HttpStatus.SC_NOT_FOUND));
  }

  @Test
  public void reject_insufficient_permission() throws Exception {
    ensureSecured();
    assumeThat("operation requires no permission", permission, is(not("none")));
    givenPermission("none")
      .param(operation, noop)
    .when()
      .get("/{language}", language)
    .then()
      .statusCode(is(HttpStatus.SC_FORBIDDEN));
  }

  @Test
  public void reject_modifying_operation_via_http_GET() throws Exception {
    assumeThat("not a modifying operation", permission, is("update"));
    givenPermission(permission)
      .param(operation, noop)
    .when()
      .get("/{language}", language)
    .then()
      .statusCode(is(HttpStatus.SC_FORBIDDEN));
  }

  @Test
  @Ignore("FIXME not implemented")
  public void reject_unknown_permission() throws Exception {
    givenPermission(permission)
      .param(operation, noop)
    .when()
      .get("/unknown/{language}", language)
    .then()
      .statusCode(is(HttpStatus.SC_NOT_FOUND));
  }

  @Test
  public void reject_operation_via_http_PUT() throws Exception {
    givenPermission(permission)
      .param(operation, noop)
    .when()
      .put("/{language}", language)
    .then()
      .statusCode(is(HttpStatus.SC_METHOD_NOT_ALLOWED));
  }

  @Test
  public void reject_operation_via_http_DELETE() throws Exception {
    givenPermission(permission)
      .param(operation, noop)
    .when()
      .delete("/{language}", language)
    .then()
      .statusCode(is(HttpStatus.SC_METHOD_NOT_ALLOWED));
  }

  @Test
  public void reject_empty_query_parameter_value() throws Exception {
    givenPermission(permission)
      .param(operation, "")
    .when()
      .get("/{language}", language)
    .then()
      .statusCode(indicates(HttpCode.CLIENT_ERROR));
  }

  @Test
  public void reject_empty_form_parameter_value() throws Exception {
    givenPermission(permission)
      .param(operation, "")
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(indicates(HttpCode.CLIENT_ERROR));
  }

  @Test
  public void reject_empty_payload_parameter_value() throws Exception {
    final String operationContentType = Pretty.format("application/%s-%s", language, operation);
    givenPermission(permission)
      .content(new byte[] {})
      .contentType(operationContentType)
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(indicates(HttpCode.CLIENT_ERROR));
  }

  @Test
  public void reject_duplicated_query_parameter() throws Exception {
    ensureReadOnly();
    givenPermission(permission)
      .param(operation, noop, noop)
    .when()
      .get("/{language}", language)
    .then()
      .statusCode(is(HttpStatus.SC_BAD_REQUEST));
  }

  @Test
  public void reject_duplicated_form_parameter() throws Exception {
    givenPermission(permission)
      .param(operation, noop, noop)
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(is(HttpStatus.SC_BAD_REQUEST));
  }

  @Test
  public void reject_malformed_payload_content_type() throws Exception {
    givenPermission(permission)
      .content(Payload.encodeUtf8(noop))
      .contentType("text/plain")
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(is(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE));
  }

  @Test
  @Ignore("cxf ignores missing content-type") // FIXME : strict mode
  public void reject_form_submission_without_form_content_type() throws Exception {
    final String form = URLEncoder.encode(operation + "=" + noop, Charsets.UTF_8.name());givenPermission(permission) // rest assured cannot serialize without content type
      .body(Payload.encodeUtf8(form))
      .header(HttpHeaders.CONTENT_TYPE, "")
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(is(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE));
  }

  @Test
  @Ignore("cf infers form type")
  public void reject_post_where_content_type_is_missing() throws Exception {
    givenPermission(permission)
      .body(Payload.encodeUtf8(noop))
      .header(HttpHeaders.CONTENT_TYPE, "")
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(is(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE));
  }

  @Test
  @Ignore("cxf handles charset")  // FIXME : strict mode
  public void reject_non_utf8_encoded_payload() throws Exception {
    final String operationContentType =
        Pretty.format("application/%s-%s; charset=UTF-16", language, operation);
    givenPermission(permission)
      .body(noop.getBytes(Charsets.UTF_16))
      .contentType(operationContentType)
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(is(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE));
  }

  @Test
  @Ignore("sql errors not translated")
  public void reject_invalid_syntax_payload() throws Exception {
    // hashing the given no-op should produce an illegal command
    givenPermission(permission)
      .param(operation, Hashing.md5().hashString(noop, Charsets.UTF_8).toString())
    .when()
      .post("/{language}", language)
    .then()
      .statusCode(indicates(HttpCode.CLIENT_ERROR));
  }
}

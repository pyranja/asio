package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.InputStream;

import static at.ac.univie.isc.asio.matcher.RestAssuredMatchers.compatibleTo;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;

@Category(Integration.class)
public class FeatureMetadata extends IntegrationTest {

  // @formatter:off

  public class Mapping {
    
    @Test
    public void endpoint_is_present() throws Exception {
      ensureDatabaseAccessible();
      ensureLanguageSupported("sparql");
      final InputStream mapping = given().role("read").and()
        .header(HttpHeaders.ACCEPT, "text/turtle")
      .when()
        .get("/mapping")
      .then()
        .statusCode(equalTo(HttpStatus.SC_OK))
        .extract().asInputStream();
      // just check response is parsable
      ModelFactory.createDefaultModel().read(mapping, null, "TURTLE");
    }
  }

  public class SchemaDescriptor {

    @Test
    public void fetch_reference_metadata() throws Exception {
      given().role("read").and()
        .header(HttpHeaders.ACCEPT, "application/json")
      .when()
        .get("/meta")
      .then()
        .statusCode(equalTo(HttpStatus.SC_OK))
        .content("identifier", equalTo("44bab742-2c27-4dad-8419-fae8a848358b"));
    }

    @Test
    public void deliver_json() throws Exception {
      given().role("read").and()
        .header(HttpHeaders.ACCEPT, "application/json")
      .when()
        .get("/meta")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(compatibleTo("application/json"))
          // must be present with value
        .body("identifier", not(isEmptyOrNullString()))
        .body("active", either(is(true)).or(is(false)))
        .body("label", not(isEmptyOrNullString()))
        .body("created", not(isEmptyOrNullString()))
        .body("updated", not(isEmptyOrNullString()))
          // optionals must be present but may be null
        .body("collect { it -> it.key }", hasItems("description", "category", "author", "license", "tags", "links"))
        .body("tags", is(any(Iterable.class)))
        .body("links", is(any(Iterable.class)))
      ;
    }

    @Test
    public void reject_unauthorized_access() throws Exception {
      ensureSecured();
      given().role("none").and()
      .when()
        .get("/meta")
      .then()
        .statusCode(is(HttpStatus.SC_FORBIDDEN));
    }
  }
}

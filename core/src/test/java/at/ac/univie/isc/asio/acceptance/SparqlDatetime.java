package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.sql.ConvertToTable;
import com.google.common.collect.Table;
import org.hamcrest.Matchers;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import java.io.InputStream;
import java.net.URI;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.CombinableMatcher.both;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class SparqlDatetime extends AcceptanceHarness {

  @Override
  protected URI getTargetUrl() {
    return readAccess().resolve("sparql");
  }

  @Test
  public void should_retrieve_single_record_with_equal_date() throws Exception {
    final String exactFilter =
        "SELECT ?id ?date WHERE { ?_ <http://test.com/integration/vocab/PUBLIC_DATETIMES_MOMENT> ?date. ?_ <http://test.com/integration/vocab/PUBLIC_DATETIMES_ID> ?id. FILTER (?date = '1984-11-28T12:00:00'^^xsd:dateTime) }";
    response = client().request(Mime.CSV.type()).post(Entity.entity(exactFilter, Mime.QUERY_SPARQL.type()));
    final Table<Integer, String, String> results =
        ConvertToTable.fromCsv(response.readEntity(InputStream.class));
    assertThat(results.rowKeySet(), hasSize(1));
    assertThat(results.row(0),
        both(hasEntry("id", "1"))
            .and(hasEntry(equalTo("date"), equalToIgnoringCase("1984-11-28T12:00:00"))));
  }

  @Test
  public void should_retrieve_all_records_with_date_before_given_date() throws Exception {
    final String exactFilter =
        "SELECT ?id ?date WHERE { ?_ <http://test.com/integration/vocab/PUBLIC_DATETIMES_MOMENT> ?date. ?_ <http://test.com/integration/vocab/PUBLIC_DATETIMES_ID> ?id. FILTER (?date < '1984-11-28T13:00:00'^^xsd:dateTime) }";
    response = client().request(Mime.CSV.type()).post(Entity.entity(exactFilter, Mime.QUERY_SPARQL.type()));
    final Table<Integer, String, String> results =
        ConvertToTable.fromCsv(response.readEntity(InputStream.class));
    assertThat(results.rowKeySet(), hasSize(2));
    assertThat(results.column("id").values(), Matchers.containsInAnyOrder("1", "3"));
  }
}

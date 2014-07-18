package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.sql.CsvToMap;
import at.ac.univie.isc.asio.sql.KeyedRow;
import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SparqlDatetime extends AcceptanceHarness {

  @Override
  protected URI getTargetUrl() {
    return AcceptanceHarness.READ_ACCESS.resolve("sparql");
  }

  @Test
  public void should_retrieve_single_record_with_equal_date() throws Exception {
    final String exactFilter =
        "SELECT ?id ?date WHERE { ?_ <http://test.com/integration/vocab/PUBLIC_DATETIMES_MOMENT> ?date. ?_ <http://test.com/integration/vocab/PUBLIC_DATETIMES_ID> ?id. FILTER (?date = '1984-11-28T12:00:00'^^xsd:dateTime) }";
    client.accept(CSV).query(PARAM_QUERY, exactFilter);
    response = client.get();
    final Map<String, KeyedRow> results =
        CsvToMap.convertStream((InputStream) response.getEntity(), "id");
    assertThat(results.size(), is(1));
    assertThat(results.keySet(), Matchers.containsInAnyOrder("1"));
    // XXX this assertion may be brittle
    assertThat(Iterables.getOnlyElement(results.values()).getColumns().get(1),
        is("1984-11-28T12:00:00"));
  }

  @Test
  public void should_retrieve_all_records_with_date_before_given_date() throws Exception {
    final String exactFilter =
        "SELECT ?id ?date WHERE { ?_ <http://test.com/integration/vocab/PUBLIC_DATETIMES_MOMENT> ?date. ?_ <http://test.com/integration/vocab/PUBLIC_DATETIMES_ID> ?id. FILTER (?date < '1984-11-28T13:00:00'^^xsd:dateTime) }";
    client.accept(CSV).query(PARAM_QUERY, exactFilter);
    response = client.get();
    final Map<String, KeyedRow> results =
        CsvToMap.convertStream((InputStream) response.getEntity(), "id");
    assertThat(results.size(), is(2));
    assertThat(results.keySet(), Matchers.containsInAnyOrder("1", "3"));
  }
}

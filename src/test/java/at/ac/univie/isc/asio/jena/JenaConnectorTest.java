package at.ac.univie.isc.asio.jena;

import at.ac.univie.isc.asio.Command;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.config.TimeoutSpec;
import at.ac.univie.isc.asio.protocol.Parameters;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.security.Token;
import at.ac.univie.isc.asio.sql.CsvToTable;
import at.ac.univie.isc.asio.tool.Unchecked;
import com.google.common.base.Charsets;
import com.google.common.collect.Table;
import com.hp.hpl.jena.query.QueryCancelledException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openjena.riot.Lang;
import rx.Scheduler;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

import static at.ac.univie.isc.asio.tool.IsIsomorphic.isomorphicWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.text.IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace;

public class JenaConnectorTest {

  public static final String WILDCARD_QUERY = "SELECT * WHERE { ?s ?p ?o }";
  public static final MediaType CSV_TYPE = MediaType.valueOf("text/csv");

  private Model model;
  private Scheduler scheduler;
  private JenaConnector subject;

  @Rule
  public ExpectedException error = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    model = ModelFactory.createDefaultModel();
    model.createResource("http://example.com/test").addProperty(RDF.value, "test-value");
    final TimeoutSpec timeout = TimeoutSpec.UNDEFINED;
    scheduler = Schedulers.immediate();
    subject = new JenaConnector(model, scheduler, timeout);
  }

  // ========= VALID QUERIES

  @Test
  public void valid_sparql_select() throws Exception {
    final Parameters params = Parameters
        .builder(Language.SPARQL)
        .single(JenaConnector.KEY_QUERY, "SELECT ?val WHERE { [] ?_ ?val }")
        .accept(CSV_TYPE)
        .build();
    final byte[] raw = executeCommandWith(params);
    final Table<Integer, String, String> result =
        CsvToTable.fromStream(new ByteArrayInputStream(raw));
    assertThat(result.size(), is(1));
    assertThat(result.get(0, "val"), is("test-value"));
  }

  @Test
  public void valid_sparql_ask() throws Exception {
    final Parameters params = Parameters.builder(Language.SPARQL)
        .single(JenaConnector.KEY_QUERY, "ASK { <http://example.com/test> ?_ 'test-value' }")
        .accept(CSV_TYPE)
        .build();
    final byte[] raw = executeCommandWith(params);
    final String result = new String(raw, Charsets.UTF_8);
    assertThat(result, is(equalToIgnoringWhiteSpace("yes")));
    // for jena 2.9.4 : assertThat(result, is("_askResulttrue"));
  }

  @Test
  public void valid_sparql_constuct() throws Exception {
    final Parameters params = Parameters.builder(Language.SPARQL)
        .single(JenaConnector.KEY_QUERY, "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }")
        .accept(MediaType.APPLICATION_XML_TYPE)
        .build();
    byte[] raw = executeCommandWith(params);
    final Model result = ModelFactory.createDefaultModel();
    result.read(new ByteArrayInputStream(raw), null, Lang.RDFXML.getName());
    assertThat(result, is(isomorphicWith(model)));
  }

  @Test
  public void valid_sparql_describe() throws Exception {
    final Parameters params = Parameters.builder(Language.SPARQL)
        .single(JenaConnector.KEY_QUERY, "DESCRIBE <http://example.com/test>")
        .accept(MediaType.TEXT_PLAIN_TYPE)
        .build();
    byte[] raw = executeCommandWith(params);
    // no standardized result format - anything suffices
    assertThat(raw.length, is(not(0)));
  }

  private byte[] executeCommandWith(final Parameters params) {
    return subject.createCommand(params, Token.ANONYMOUS)
        .observe()
        .map(new Func1<StreamingOutput, byte[]>() {
          @Override
          public byte[] call(final StreamingOutput streamingOutput) {
            final ByteArrayOutputStream sink = new ByteArrayOutputStream();
            Unchecked.write(streamingOutput, sink);
            return sink.toByteArray();
          }
        })
        .toBlocking().single();
  }

  // ========= FUNCTIONALITY

  @Test
  public void should_require_read_role() throws Exception {
    final Parameters params = Parameters.builder(Language.SPARQL)
        .single(JenaConnector.KEY_QUERY, WILDCARD_QUERY)
        .accept(MediaType.WILDCARD_TYPE).build();
    final Command command = subject.createCommand(params, Token.ANONYMOUS);
    assertThat(command.requiredRole(), is(Role.READ));
  }

  @Test
  public void should_use_sparql_results_format_if_xml_accepted() throws Exception {
    final Parameters params = Parameters.builder(Language.SPARQL)
        .single(JenaConnector.KEY_QUERY, WILDCARD_QUERY)
        .accept(MediaType.APPLICATION_XML_TYPE).build();
    final Command command = subject.createCommand(params, Token.ANONYMOUS);
    assertThat(command.format(), is(MediaType.valueOf("application/sparql-results+xml")));
  }

  @Test
  public void should_use_sparql_results_format_as_default() throws Exception {
    final Parameters params = Parameters.builder(Language.SPARQL)
        .single(JenaConnector.KEY_QUERY, WILDCARD_QUERY)
        .accept(MediaType.WILDCARD_TYPE).build();
    final Command command = subject.createCommand(params, Token.ANONYMOUS);
    assertThat(command.format(), is(MediaType.valueOf("application/sparql-results+xml")));
  }

  @Test
  public void should_set_timeout_on_query() throws Exception {
    subject = new JenaConnector(model, scheduler, TimeoutSpec.from(1, TimeUnit.MILLISECONDS));
    final Command command = subject.createCommand(Parameters.builder(Language.SPARQL)
            .single(JenaConnector.KEY_QUERY, WILDCARD_QUERY)
            .accept(MediaType.WILDCARD_TYPE).build(),
        Token.ANONYMOUS);
    error.expect(QueryCancelledException.class);
    command.observe().toBlocking().single();
  }

  @Test
  public void should_add_owner_credentials_to_query() throws Exception {
    final Parameters params = Parameters.builder(Language.SPARQL)
        .single(JenaConnector.KEY_QUERY, WILDCARD_QUERY)
        .accept(MediaType.WILDCARD_TYPE).build();
    final Command command = subject.createCommand(params, Token.from("test-user", "test-token"));
    final Context context = ((JenaConnector.JenaCommand) command).execution().getContext();
    // no username in VPH auth
    assertThat(context.getAsString(JenaConnector.CONTEXT_AUTH_USERNAME), is(""));
    assertThat(context.getAsString(JenaConnector.CONTEXT_AUTH_PASSWORD), is("test-token"));
  }

  @Test
  public void should_skip_credentials_delegation_if_anonymous() throws Exception {
    final Parameters params = Parameters.builder(Language.SPARQL)
        .single(JenaConnector.KEY_QUERY, WILDCARD_QUERY)
        .accept(MediaType.WILDCARD_TYPE).build();
    final Command command = subject.createCommand(params, Token.ANONYMOUS);
    final Context context = ((JenaConnector.JenaCommand) command).execution().getContext();
    assertThat(context.getAsString(JenaConnector.CONTEXT_AUTH_USERNAME), is(nullValue()));
    assertThat(context.getAsString(JenaConnector.CONTEXT_AUTH_PASSWORD), is(nullValue()));
  }

  // ========= ILLEGAL INPUT

  @Test
  public void fail_on_missing_query() throws Exception {
    final Parameters params =
        Parameters.builder(Language.SPARQL).accept(MediaType.APPLICATION_XML_TYPE).build();
    error.expect(DatasetUsageException.class);
    subject.createCommand(params, Token.ANONYMOUS);
  }

  @Test
  public void fail_on_multiple_queries() throws Exception {
    final Parameters params =
        Parameters.builder(Language.SPARQL).accept(MediaType.APPLICATION_XML_TYPE)
            .single(JenaConnector.KEY_QUERY, "one").single(JenaConnector.KEY_QUERY, "two").build();
    error.expect(DatasetUsageException.class);
    subject.createCommand(params, Token.ANONYMOUS);
  }

  @Test
  public void should_fail_if_no_acceptable_format_given() throws Exception {
    final Parameters params = Parameters.builder(Language.SPARQL)
        .single(JenaConnector.KEY_QUERY, WILDCARD_QUERY).build();
    error.expect(JenaConnector.NoSupportedFormat.class);
    subject.createCommand(params, Token.ANONYMOUS);
  }

  @Test
  public void should_fail_if_no_supported_format_is_given() throws Exception {
    final Parameters params = Parameters.builder(Language.SPARQL)
        .single(JenaConnector.KEY_QUERY, WILDCARD_QUERY)
        .accept(MediaType.valueOf("image/jpg")).build();
    error.expect(JenaConnector.NoSupportedFormat.class);
    subject.createCommand(params, Token.ANONYMOUS);
  }
}

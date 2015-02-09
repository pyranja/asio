package at.ac.univie.isc.asio.engine.sparql;

import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.engine.*;
import at.ac.univie.isc.asio.security.Token;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.sql.ConvertToTable;
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

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static at.ac.univie.isc.asio.junit.IsIsomorphic.isomorphicWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.text.IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace;

public class JenaEngineTest {

  public static final String WILDCARD_QUERY = "SELECT * WHERE { ?s ?p ?o }";
  public static final MediaType CSV_TYPE = MediaType.valueOf("text/csv");

  private Model model;
  private JenaEngine subject;

  @Rule
  public ExpectedException error = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    model = ModelFactory.createDefaultModel();
    model.createResource("http://example.com/test").addProperty(RDF.value, "test-value");
    final TimeoutSpec timeout = TimeoutSpec.undefined();
    subject = JenaEngine.create(model, timeout, false);
  }

  // ========= VALID QUERIES

  @Test
  public void valid_sparql_select() throws Exception {
    final Command params = CommandBuilder
        .with(Language.SPARQL)
        .single(JenaEngine.KEY_QUERY, "SELECT ?val WHERE { [] ?_ ?val }")
        .accept(CSV_TYPE)
        .build();
    final byte[] raw = executeCommandWith(params);
    final Table<Integer, String, String> result =
        ConvertToTable.fromCsv(new ByteArrayInputStream(raw));
    assertThat(result.size(), is(1));
    assertThat(result.get(0, "val"), is("test-value"));
  }

  @Test
  public void valid_sparql_ask() throws Exception {
    final Command params = CommandBuilder.with(Language.SPARQL)
        .single(JenaEngine.KEY_QUERY, "ASK { <http://example.com/test> ?_ 'test-value' }")
        .accept(CSV_TYPE)
        .build();
    final byte[] raw = executeCommandWith(params);
    final String result = new String(raw, Charsets.UTF_8);
    assertThat(result, is(equalToIgnoringWhiteSpace("_askResult true")));
    // for jena 2.9.4 : expect "_askResult true"
    // pre jena 2.9.4 : expect "yes"
  }

  @Test
  public void valid_sparql_constuct() throws Exception {
    final Command params = CommandBuilder.with(Language.SPARQL)
        .single(JenaEngine.KEY_QUERY, "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }")
        .accept(MediaType.APPLICATION_XML_TYPE)
        .build();
    byte[] raw = executeCommandWith(params);
    final Model result = ModelFactory.createDefaultModel();
    result.read(new ByteArrayInputStream(raw), null, Lang.RDFXML.getName());
    assertThat(result, is(isomorphicWith(model)));
  }

  @Test
  public void valid_sparql_describe() throws Exception {
    final Command params = CommandBuilder.with(Language.SPARQL)
        .single(JenaEngine.KEY_QUERY, "DESCRIBE <http://example.com/test>")
        .accept(MediaType.TEXT_PLAIN_TYPE)
        .build();
    byte[] raw = executeCommandWith(params);
    // no standardized result format - anything suffices
    assertThat(raw.length, is(not(0)));
  }

  private byte[] executeCommandWith(final Command params) throws IOException {
    final SparqlInvocation invocation = subject.prepare(params);
    invocation.execute();
    final ByteArrayOutputStream sink = new ByteArrayOutputStream();
    invocation.write(sink);
    return sink.toByteArray();
  }

  // ========= FUNCTIONALITY

  @Test
  public void should_require_read_role() throws Exception {
    final Command params = CommandBuilder.with(Language.SPARQL)
        .single(JenaEngine.KEY_QUERY, WILDCARD_QUERY)
        .accept(MediaType.WILDCARD_TYPE).build();
    final SparqlInvocation invocation = subject.prepare(params);
    assertThat(invocation.requires(), is(Role.READ));
  }

  @Test
  public void should_use_sparql_results_format_if_xml_accepted() throws Exception {
    final Command params = CommandBuilder.with(Language.SPARQL)
        .single(JenaEngine.KEY_QUERY, WILDCARD_QUERY)
        .accept(MediaType.APPLICATION_XML_TYPE).build();
    final SparqlInvocation invocation = subject.prepare(params);
    assertThat(invocation.produces(), is(MediaType.valueOf("application/sparql-results+xml")));
  }

  @Test
  public void should_use_sparql_results_format_as_default() throws Exception {
    final Command params = CommandBuilder.with(Language.SPARQL)
        .single(JenaEngine.KEY_QUERY, WILDCARD_QUERY)
        .accept(MediaType.WILDCARD_TYPE).build();
    final SparqlInvocation invocation = subject.prepare(params);
    assertThat(invocation.produces(), is(MediaType.valueOf("application/sparql-results+xml")));
  }

  @Test
  public void should_set_timeout_on_query() throws Exception {
    subject = JenaEngine.create(model, TimeoutSpec.from(1, TimeUnit.MILLISECONDS), false);
    final SparqlInvocation invocation = subject.prepare(CommandBuilder.with(Language.SPARQL)
            .single(JenaEngine.KEY_QUERY, WILDCARD_QUERY)
            .accept(MediaType.WILDCARD_TYPE).build()
    );
    error.expect(QueryCancelledException.class);
    invocation.execute();
  }

  @Test
  public void should_add_owner_credentials_to_query() throws Exception {
    final Command params = CommandBuilder.with(Language.SPARQL)
        .single(JenaEngine.KEY_QUERY, WILDCARD_QUERY)
        .accept(MediaType.WILDCARD_TYPE)
        .owner(Token.from("test-user", "test-token")).build();
    final SparqlInvocation invocation = subject.prepare(params);
    final Context context = invocation.query().getContext();
    // no username in VPH auth
    assertThat(context.getAsString(DefaultJenaFactory.CONTEXT_AUTH_USERNAME), is(""));
    assertThat(context.getAsString(DefaultJenaFactory.CONTEXT_AUTH_PASSWORD), is("test-token"));
  }

  @Test
  public void should_skip_credentials_delegation_if_anonymous() throws Exception {
    final Command params = CommandBuilder.with(Language.SPARQL)
        .single(JenaEngine.KEY_QUERY, WILDCARD_QUERY)
        .accept(MediaType.WILDCARD_TYPE)
        .owner(Token.undefined()).build();
    final SparqlInvocation invocation = subject.prepare(params);
    final Context context = invocation.query().getContext();
    assertThat(context.getAsString(DefaultJenaFactory.CONTEXT_AUTH_USERNAME), is(nullValue()));
    assertThat(context.getAsString(DefaultJenaFactory.CONTEXT_AUTH_PASSWORD), is(nullValue()));
  }

  // ========= ILLEGAL INPUT

  @Test
  public void fail_on_missing_query() throws Exception {
    final Command params =
        CommandBuilder.with(Language.SPARQL).accept(MediaType.APPLICATION_XML_TYPE).build();
    error.expect(DatasetUsageException.class);
    subject.prepare(params);
  }

  @Test
  public void fail_on_multiple_queries() throws Exception {
    final Command params =
        CommandBuilder.with(Language.SPARQL).accept(MediaType.APPLICATION_XML_TYPE)
            .single(JenaEngine.KEY_QUERY, "one").single(JenaEngine.KEY_QUERY, "two").build();
    error.expect(DatasetUsageException.class);
    subject.prepare(params);
  }

  @Test
  public void should_fail_if_no_acceptable_format_given() throws Exception {
    final Command params = CommandBuilder.with(Language.SPARQL)
        .single(JenaEngine.KEY_QUERY, WILDCARD_QUERY).build();
    error.expect(TypeMatchingResolver.NoMatchingFormat.class);
    subject.prepare(params);
  }

  @Test
  public void should_fail_if_no_supported_format_is_given() throws Exception {
    final Command params = CommandBuilder.with(Language.SPARQL)
        .single(JenaEngine.KEY_QUERY, WILDCARD_QUERY)
        .accept(MediaType.valueOf("image/jpg")).build();
    error.expect(TypeMatchingResolver.NoMatchingFormat.class);
    subject.prepare(params);
  }

  @Test
  public void should_reject_federated_query_if_federated_query_lock_in_place() throws Exception {
    subject = JenaEngine.create(model, TimeoutSpec.undefined(), false);
    final String fedQuery = "SELECT * WHERE { SERVICE <http://example.com> { ?s ?p ?o } }";
    final Command params = CommandBuilder.with(Language.SPARQL)
        .single(JenaEngine.KEY_QUERY, fedQuery)
        .accept(MediaType.WILDCARD_TYPE).build();
    error.expect(DatasetUsageException.class);
    subject.prepare(params);
  }
}

package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.MockFormat;
import at.ac.univie.isc.asio.MockResult;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.tool.EmbeddedJaxrsServer;
import at.ac.univie.isc.asio.tool.JaxrsClientProvider;
import at.ac.univie.isc.asio.tool.ResponseMonitor;
import at.ac.univie.isc.asio.transport.ObservableStream;
import com.google.common.io.ByteStreams;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static at.ac.univie.isc.asio.tool.JaxrsResponseMatchers.hasStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Verify the correct implementation of the SPARQL/(SQL) Protocol recommendation, w.r.t. to asio.
 * 
 * @author Chris Borckholder
 */
@RunWith(MockitoJUnitRunner.class)
public class ProtocolAcceptanceTest {

  private static final MockProtocolApplication application = new MockProtocolApplication();

  @ClassRule
  public static EmbeddedJaxrsServer server = new EmbeddedJaxrsServer(application);

  public JaxrsClientProvider provider = new JaxrsClientProvider(server.getBaseUri());

  @Rule
  public TestRule chain = RuleChain.outerRule(provider).around(new ResponseMonitor(provider));
  @Rule
  public Timeout timeout = new Timeout(50000);

  private static final String PAYLOAD = "SELECT * FROM test_table";

  private WebClient client;
  private Response response;
  @Captor
  private ArgumentCaptor<DatasetOperation> operation;
  // mock
  private Engine acceptor;

  @Before
  public void setUp() {
    client = provider.get().path("/");
    client.accept(MockFormat.APPLICABLE_CONTENT_TYPE);
    acceptor = application.getAcceptor();
    final ByteArrayInputStream source = new ByteArrayInputStream(MockResult.PAYLOAD);
    when(acceptor.execute(operation.capture())).thenReturn(Observable.just(ObservableStream.from(source)).subscribeOn(Schedulers.io()));
  }

  @After
  public void tearDown() {
    application.reset();
  }

  // ====================================================================================>
  // VALID OPERATIONS

  @Test
  public void should_accept_query_through_get_request() throws Exception {
    client.query("query", PAYLOAD);
    response = client.invoke("GET", null);
    verifySuccess(response);
    verifyOperation(Action.QUERY);
  }

  @Test
  public void should_accept_query_through_form_request() throws Exception {
    final Form params = new Form();
    params.param("query", PAYLOAD);
    response = client.form(params);
    verifySuccess(response);
    verifyOperation(Action.QUERY);
  }

  @Test
  public void should_accept_update_through_form_request() throws Exception {
    final Form params = new Form();
    params.param("update", PAYLOAD);
    response = client.form(params);
    verifySuccess(response);
    verifyOperation(Action.UPDATE);
  }

  @Test
  public void should_accept_post_with_raw_query_as_body() throws Exception {
    client.type("application/test-query");
    response = client.post(PAYLOAD);
    verifySuccess(response);
    verifyOperation(Action.QUERY);
  }

  @Test
  public void should_accept_post_with_raw_update_as_body() throws Exception {
    client.type("application/test-update");
    response = client.post(PAYLOAD);
    verifySuccess(response);
    verifyOperation(Action.UPDATE);
  }

  private void verifyOperation(final Action expected) throws DatasetUsageException {
    final DatasetOperation received = operation.getValue();
    assertThat(received.action(), is(expected));
    assertThat(received.commandOrFail(), is(PAYLOAD));
  }

  private void verifySuccess(final Response response) throws IOException {
    assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    try (final InputStream body = (InputStream) response.getEntity()) {
      assertThat(ByteStreams.toByteArray(body), is(MockResult.PAYLOAD));
    }
  }

  // ====================================================================================>
  // INVALID OPERATIONS

  @Test
  public void should_reject_get_if_query_command_is_missing() throws Exception {
    response = client.get();
    verifyFailure(response);
  }


  @Test
  public void should_reject_get_if_query_command_is_duplicated() throws Exception {
    client.query("query", PAYLOAD).query("query", "DUPLICATE");
    response = client.get();
    verifyFailure(response);
  }

  @Test
  public void should_reject_get_with_update_parameter() throws Exception {
    client.query("update", PAYLOAD);
    response = client.get();
    verifyFailure(response);
  }

  @Test
  public void should_reject_form_if_no_command_given() throws Exception {
    final Form params = new Form();
    params.param("other", "ignored"); // XXX CXF < 2.7.6 : posting empty form will trigger HTTP error
    response = client.form(params);
    verifyFailure(response);
  }

  @Test
  public void should_reject_form_with_duplicated_command() throws Exception {
    final Form params = new Form();
    params.param("query", "a query");
    params.param("query", "duplicate");
    response = client.form(params);
    verifyFailure(response);
  }

  @Test
  public void should_reject_form_if_both_commands_given() throws Exception {
    final Form params = new Form();
    params.param("query", "a query");
    params.param("update", "an update");
    response = client.form(params);
    verifyFailure(response);
  }

  @Test
  public void should_reject_raw_post_with_unexpected_content_type() throws Exception {
    client.type("text/plain");
    response = client.post(PAYLOAD);
    verifyFailure(response);
  }

  @Test
  public void should_reject_raw_post_with_unsupported_command_in_content_type() throws Exception {
    client.type("application/test-delete");
    response = client.post(PAYLOAD);
    verifyFailure(response);
  }

  private void verifyFailure(final Response response) {
    verify(acceptor, never()).execute(any(DatasetOperation.class));
    assertThat(Status.Family.familyOf(response.getStatus()), is(Status.Family.CLIENT_ERROR));
  }

  // ====================================================================================>
  // ERROR HANDLING

  @Test
  public void should_fail_with_timeout_if_engine_not_responding() throws Exception {
    when(acceptor.execute(operation.capture())).thenReturn(Observable.<ObservableStream>never());
    client.type("application/test-query");
    response = client.post(PAYLOAD);
    verify(acceptor).execute(any(DatasetOperation.class));
    assertThat(response, hasStatus(Status.SERVICE_UNAVAILABLE));
  }
}

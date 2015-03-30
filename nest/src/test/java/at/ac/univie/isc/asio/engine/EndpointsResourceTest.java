package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.Mime;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.jaxrs.AsyncResponseFake;
import at.ac.univie.isc.asio.security.Identity;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import rx.Observable;

import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static at.ac.univie.isc.asio.junit.IsMultimapContaining.hasEntries;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EndpointsResourceTest {
  private final Connector connector = Mockito.mock(Connector.class);
  private final EndpointsResource subject = new EndpointsResource(connector, TimeoutSpec.undefined());

  private final HttpHeaders headers = Mockito.mock(HttpHeaders.class);
  private final SecurityContext security = Mockito.mock(SecurityContext.class);

  private final AsyncResponseFake async = AsyncResponseFake.create();
  private final MultivaluedMap<String, String> requestParameters = new MultivaluedHashMap<>();
  private final byte[] payload = Payload.randomWithLength(100);

  private EndpointsResource.Params request = new EndpointsResource.Params();

  @Before
  public void setUp() throws Exception {
    when(connector.accept(any(Command.class))).thenReturn(streamedResultsFrom(payload));
    request.id = Id.valueOf("test");
    request.language = Language.valueOf("test");
    request.headers = headers;
    request.security = security;
  }

  // ===============================================================================================
  // HAPPY PATH

  @Test
  public void valid_get_operation_should_succeed() throws Exception {
    final UriInfo uri = Mockito.mock(UriInfo.class);
    when(uri.getQueryParameters()).thenReturn(requestParameters);
    requestParameters.addFirst("operation", "command");
    subject.acceptQuery(uri, async, request);
    assertThatResponseIsSuccessful();
  }

  @Test
  public void valid_form_operation_should_succeed() throws Exception {
    requestParameters.addFirst("operation", "command");
    subject.acceptForm(requestParameters, async, request);
    assertThatResponseIsSuccessful();
  }

  @Test
  public void valid_body_operation_should_succeed() throws Exception {
    subject.acceptBody("command", MediaType.valueOf("application/test-operation"), async, request);
    assertThatResponseIsSuccessful();
  }

  private void assertThatResponseIsSuccessful() {
    assertThat(async.response(), hasStatus(Response.Status.OK));
    assertThat(async.response().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
    assertThat(async.entity(byte[].class), is(payload));
  }

  // ===============================================================================================
  // INVOKING CONNECTOR

  private final ArgumentCaptor<Command> params = ArgumentCaptor.forClass(Command.class);

  @Test
  public void forward_request_language() throws Exception {
    subject.acceptForm(requestParameters, async, request);
    verify(connector).accept(params.capture());
    assertThat(params.getValue().language(), is(Language.valueOf("test")));
    assertThat(params.getValue().properties(), hasEntries(is("language"), contains(equalToIgnoringCase("test"))));
  }

  @Test
  public void forward_request_schema() throws Exception {
    subject.acceptForm(requestParameters, async, request);
    verify(connector).accept(params.capture());
    assertThat(params.getValue().schema(), is(Id.valueOf("test")));
    assertThat(params.getValue().properties(), hasEntries(is("schema"), contains(equalToIgnoringCase("test"))));
  }

  @Test
  public void forward_accepted_types() throws Exception {
    final List<MediaType> expected = Arrays.asList(MediaType.APPLICATION_JSON_TYPE);
    when(headers.getAcceptableMediaTypes()).thenReturn(expected);
    subject.acceptForm(requestParameters, async, request);
    verify(connector).accept(params.capture());
    assertThat(params.getValue().acceptable(), is(expected));
  }

  @Test
  public void forward_request_principal() throws Exception {
    when(security.getUserPrincipal()).thenReturn(Identity.from("test", "password"));
    subject.acceptForm(requestParameters, async, request);
    verify(connector).accept(params.capture());
    assertThat(params.getValue().owner().get(), Matchers.<Principal>is(Identity.from("test", "password")));
  }

  @Test
  public void use_undefined_principal_if_missing() throws Exception {
    when(security.getUserPrincipal()).thenReturn(null);
    subject.acceptForm(requestParameters, async, request);
    verify(connector).accept(params.capture());
    assertThat(params.getValue().owner().get(), Matchers.<Principal>is(Identity.undefined()));
  }

  @Test
  public void forward_all_query_parameters() throws Exception {
    final UriInfo uri = Mockito.mock(UriInfo.class);
    when(uri.getQueryParameters()).thenReturn(requestParameters);
    requestParameters.addAll("one", "1");
    requestParameters.addAll("two", "2", "3");
    subject.acceptQuery(uri, async, request);
    verify(connector).accept(params.capture());
    assertThat(params.getValue().properties(), hasEntries("one", "1"));
    assertThat(params.getValue().properties(), hasEntries("two", "2", "3"));
  }

  @Test
  public void forward_all_form_parameters() throws Exception {
    requestParameters.addAll("one", "1");
    requestParameters.addAll("two", "2", "3");
    subject.acceptForm(requestParameters, async, request);
    verify(connector).accept(params.capture());
    assertThat(params.getValue().properties(), hasEntries("one", "1"));
    assertThat(params.getValue().properties(), hasEntries("two", "2", "3"));
  }

  @Test
  public void forward_body_parameter() throws Exception {
    subject.acceptBody("1", MediaType.valueOf("application/test-operation"), async, request);
    verify(connector).accept(params.capture());
    assertThat(params.getValue().properties(), hasEntries("operation", "1"));
  }

  // ===============================================================================================
  // INTERNAL ERRORS

  @Test
  public void send_error_if_connector_fails_fatally() throws Exception {
    final Throwable failure = new DatasetFailureException(new IllegalStateException());
    when(connector.accept(any(Command.class))).thenThrow(failure);
    try {
      subject.acceptBody("command", Mime.QUERY_SQL.type(), async, request);
    } catch (Exception ignored) {
    }
    assertThat(async.error(), is(failure));
  }

  @Test
  public void send_error_if_observable_fails() throws Exception {
    final Throwable failure = new DatasetFailureException(new IllegalStateException());
    when(connector.accept(any(Command.class))).thenReturn(Observable.<StreamedResults>error(failure));
    subject.acceptBody("command", Mime.QUERY_SQL.type(), async, request);
    assertThat(async.error(), is(failure));
  }

  private Observable<StreamedResults> streamedResultsFrom(final byte[] payload) {
    return Observable.<StreamedResults>just(new StreamedResults(MediaType.APPLICATION_JSON_TYPE) {
      @Override
      protected void doWrite(final OutputStream output) throws IOException {
        output.write(payload);
      }
    });
  }
}

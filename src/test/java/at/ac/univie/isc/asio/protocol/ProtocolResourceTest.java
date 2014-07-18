package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.*;
import at.ac.univie.isc.asio.config.JaxrsSpec;
import at.ac.univie.isc.asio.config.TimeoutSpec;
import at.ac.univie.isc.asio.jaxrs.AcceptTunnelFilter;
import at.ac.univie.isc.asio.jaxrs.EmbeddedServer;
import at.ac.univie.isc.asio.security.Permission;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.security.Token;
import at.ac.univie.isc.asio.tool.Rules;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.ArgumentCaptor;
import rx.Observable;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.concurrent.TimeUnit;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasBody;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static at.ac.univie.isc.asio.tool.IsMultimapContaining.hasEntries;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ProtocolResourceTest {
  private final TimeoutSpec timeout = mock(TimeoutSpec.class);
  private final Connector connector = mock(Connector.class);
  private final Command command = mock(Command.class);

  private final byte[] PAYLOAD = "{response : success}".getBytes(Charsets.UTF_8);

  @Rule
  public Timeout testTimeout = Rules.timeout(2, TimeUnit.SECONDS);
  @Rule
  public EmbeddedServer server = EmbeddedServer
      .host(JaxrsSpec.create(ProtocolResource.class))
      .resource(new ProtocolResource(connector, timeout))
      .enableLogging()
      .create();

  private Response response;

  @Before
  public void setup() {
    when(timeout.getAs(any(TimeUnit.class))).thenReturn(-1L);
    // prepare mocks
    when(connector.createCommand(any(Parameters.class), any(Principal.class)))
        .thenReturn(command);
    when(command.format()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
    when(command.requiredRole()).thenReturn(Role.ANY);
    final Command.Results payloadStreamer = new Command.Results() {
      @Override
      public void write(final OutputStream output) throws IOException, WebApplicationException {
        output.write(PAYLOAD);
      }

      @Override
      public void close() {}
    };
    when(command.observe()).thenReturn(Observable.from(payloadStreamer));
  }

  private WebTarget invoke(final Permission permission, final Language language) {
    return server.endpoint().path("{permission}").path("{language}")
        .resolveTemplate("permission", permission.name())
        .resolveTemplate("language", language.name());
  }

  // ====================================================================================>
  // HAPPY PATH

  @Test
  public void valid_get_operation() throws Exception {
    response = invoke(Permission.READ, Language.SQL)
        .queryParam("action", "command")
        .request(MediaType.APPLICATION_JSON)
        .get();
    verifySuccessful(response);
  }

  @Test
  public void valid_form_operation() throws Exception {
    final Form form = new Form("action", "command");
    response = invoke(Permission.READ, Language.SQL)
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    verifySuccessful(response);
  }

  @Test
  public void valid_body_operation() throws Exception {
    response = invoke(Permission.READ, Language.SQL)
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.entity("command", MediaType.valueOf("application/sql-action")));
    verifySuccessful(response);
  }

  @Test
  public void valid_schema_operation() throws Exception {
    response = server.endpoint()
        .path("read/sql/schema")
        .request(MediaType.APPLICATION_JSON)
        .get();
    verifySuccessful(response);
  }

  private void verifySuccessful(final Response response) {
    assertThat(response, hasStatus(Response.Status.OK));
    assertThat(response.getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
    assertThat(response, hasBody(PAYLOAD));
  }

  // ====================================================================================>
  // ILLEGAL REQUESTS

  @Test
  public void body_operation_malformed_media_type() throws Exception {
    response = invoke(Permission.READ, Language.SQL)
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.entity("command", MediaType.TEXT_PLAIN_TYPE));
    assertThat(response, hasStatus(Response.Status.UNSUPPORTED_MEDIA_TYPE));
  }

  @Test
  public void body_operation_language_mismatch() throws Exception {
    response = invoke(Permission.READ, Language.SQL)
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.entity("command", MediaType.valueOf("application/sparql-action")));
    assertThat(response, hasStatus(Response.Status.UNSUPPORTED_MEDIA_TYPE));
  }

  // ====================================================================================>
  // INTERNAL INVOCATION

  private final ArgumentCaptor<Parameters> params = ArgumentCaptor.forClass(Parameters.class);

  @Test
  public void forward_all_parameters_from_get_operation() throws Exception {
    invoke(Permission.READ, Language.SQL)
        .queryParam("one", "1")
        .queryParam("two", "2")
        .queryParam("two", "2")
        .request(MediaType.APPLICATION_JSON).get();
    verify(connector).createCommand(params.capture(), any(Principal.class));
    assertThat(params.getValue().properties(), hasEntries("one", "1"));
    assertThat(params.getValue().properties(), hasEntries("two", "2", "2"));
  }

  @Test
  public void forward_all_parameters_from_form_operation() throws Exception {
    final Form form = new Form("two", "2").param("two","2").param("one", "1");
    invoke(Permission.READ, Language.SQL).request(MediaType.APPLICATION_JSON)
        .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    verify(connector).createCommand(params.capture(), any(Principal.class));
    assertThat(params.getValue().properties(), hasEntries("one", "1"));
    assertThat(params.getValue().properties(), hasEntries("two", "2", "2"));
  }

  @Test
  public void forward_parameter_from_body_operation() throws Exception {
    invoke(Permission.READ, Language.SQL).request(MediaType.APPLICATION_JSON)
        .post(Entity.entity("1", MediaType.valueOf("application/sql-one")));
    verify(connector).createCommand(params.capture(), any(Principal.class));
    assertThat(params.getValue().properties(), hasEntries("one", "1"));
  }

  @Test
  public void forward_language() throws Exception {
    invoke(Permission.READ, Language.SQL).request(MediaType.APPLICATION_JSON).get();
    verify(connector).createCommand(params.capture(), any(Principal.class));
    assertThat(params.getValue().language(), is(Language.SQL));
  }

  @Test
  public void forward_acceptable_types() throws Exception {
    invoke(Permission.READ, Language.SQL)
        .request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN).get();
    verify(connector).createCommand(params.capture(), any(Principal.class));
    assertThat(params.getValue().acceptable(), containsInAnyOrder(
            MediaType.TEXT_PLAIN_TYPE,
            MediaType.APPLICATION_XML_TYPE,
            MediaType.APPLICATION_JSON_TYPE));
  }

  @Test
  public void default_to_xml_if_accept_header_missing() throws Exception {
    invoke(Permission.READ, Language.SQL).request().header(HttpHeaders.ACCEPT, null).get();
    verify(connector).createCommand(params.capture(), any(Principal.class));
    assertThat(params.getValue().acceptable(), containsInAnyOrder(MediaType.APPLICATION_XML_TYPE));
  }

  @Test
  public void replace_accept_header_with_tunneled_type() throws Exception {
    // .queryParam("_type", "xml") works also -> cxf built-in RequestPreprocessor
    invoke(Permission.READ, Language.SQL)
        .queryParam(AcceptTunnelFilter.ACCEPT_PARAM_TUNNEL, MediaType.TEXT_HTML)
        .request(MediaType.APPLICATION_JSON).get();
    verify(connector).createCommand(params.capture(), any(Principal.class));
    assertThat(params.getValue().acceptable(), containsInAnyOrder(MediaType.TEXT_HTML_TYPE));
  }

  private final ArgumentCaptor<Principal> principal = ArgumentCaptor.forClass(Principal.class);

  @Test
  public void forward_authorization_token() throws Exception {
    final String authText = "Basic " + BaseEncoding.base64().encode("test:password".getBytes(Charsets.UTF_8));
    invoke(Permission.READ, Language.SQL).request(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.AUTHORIZATION, authText).get();
    verify(connector).createCommand(any(Parameters.class), principal.capture());
    final Token token = (Token) principal.getValue();
    assertThat(token.getName(), is("test"));
    assertThat(token.getToken(), is("password"));
  }

  // ====================================================================================>
  // INTERNAL ERRORS

  @Test
  public void language_is_not_supported() throws Exception {
    when(connector.createCommand(any(Parameters.class), any(Principal.class)))
        .thenThrow(new Connector.LanguageNotSupported(Language.SQL));
    response = invoke(Permission.READ, Language.SQL).request().get();
    assertThat(response, hasStatus(Response.Status.NOT_FOUND));
  }

  @Test
  public void operation_with_insufficient_authorization() throws Exception {
    when(command.requiredRole()).thenReturn(Role.WRITE);
    response = invoke(Permission.READ, Language.SQL).request().get();
    assertThat(response, hasStatus(Response.Status.FORBIDDEN));
  }

  @Test
  public void get_operation_requires_write_role() throws Exception {
    when(command.requiredRole()).thenReturn(Role.WRITE);
    response = invoke(Permission.FULL, Language.SQL).request().get();
    assertThat(response, hasStatus(Response.Status.FORBIDDEN));
  }

  @Test
  public void command_execution_fails_fatally() throws Exception {
    when(command.observe()).thenThrow(IllegalStateException.class);
    response = invoke(Permission.READ, Language.SQL).request().get();
    assertThat(response, hasStatus(Response.Status.INTERNAL_SERVER_ERROR));
  }

  @Test
  public void observable_fails_due_to_client_error() throws Exception {
    when(command.observe())
        .thenReturn(Observable.<Command.Results>error(new DatasetUsageException("test-exception")));
    response = invoke(Permission.READ, Language.SQL).request().get();
    assertThat(response, hasStatus(Response.Status.BAD_REQUEST));
  }

  @Test
  public void observable_fails_due_to_internal_error() throws Exception {
    when(command.observe())
        .thenReturn(Observable.<Command.Results>error(new DatasetFailureException(new IllegalStateException())));
    response = invoke(Permission.READ, Language.SQL).request().get();
    assertThat(response, hasStatus(Response.Status.INTERNAL_SERVER_ERROR));
  }

  @Test
  public void execution_times_out() throws Exception {
    when(timeout.getAs(TimeUnit.NANOSECONDS)).thenReturn(TimeUnit.NANOSECONDS.convert(100, TimeUnit.MILLISECONDS));
    when(command.observe()).thenReturn(Observable.<Command.Results>never());
    response = invoke(Permission.READ, Language.SQL).request().get();
    assertThat(response, hasStatus(Response.Status.SERVICE_UNAVAILABLE));
  }

  @Test
  public void serialization_fails_fatally() throws Exception {
    final Command.Results failing = new Command.Results() {
      @Override
      public void write(final OutputStream output) throws IOException, WebApplicationException {
        throw new DatasetFailureException(new IllegalStateException());
      }

      @Override
      public void close() {}
    };
    when(command.observe()).thenReturn(Observable.just(failing));
    response = invoke(Permission.READ, Language.SQL).request().get();
    assertThat(response, hasStatus(Response.Status.INTERNAL_SERVER_ERROR));
  }

  @Test
  public void serialization_fails_with_web_error() throws Exception {
    final Command.Results failing = new Command.Results() {
      @Override
      public void write(final OutputStream output) throws IOException, WebApplicationException {
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
      }

      @Override
      public void close() {}
    };
    when(command.observe()).thenReturn(Observable.just(failing));
    response = invoke(Permission.READ, Language.SQL).request().get();
    assertThat(response, hasStatus(Response.Status.BAD_REQUEST));
  }
}

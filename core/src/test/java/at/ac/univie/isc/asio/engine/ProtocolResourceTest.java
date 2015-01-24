package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.Payload;
import at.ac.univie.isc.asio.jaxrs.AsyncResponseFake;
import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import rx.Observable;

import javax.ws.rs.core.*;

import java.io.IOException;
import java.io.OutputStream;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasBody;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static at.ac.univie.isc.asio.junit.IsMultimapContaining.hasEntries;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProtocolResourceTest {
  private final Connector connector = Mockito.mock(Connector.class);
  private final ProtocolResource subject =
      new ProtocolResource(ParseJaxrsParameters.with(Language.valueOf("sql")), connector, TimeoutSpec.undefined());

  private final AsyncResponseFake async = AsyncResponseFake.create();
  private final MultivaluedMap<String, String> requestParameters = new MultivaluedHashMap<>();
  private final byte[] payload = Payload.randomWithLength(100);

  @Before
  public void setUp() throws Exception {
    when(connector.accept(any(Parameters.class))).thenReturn(streamedResultsFrom(payload));
  }

  // ===============================================================================================
  // HAPPY PATH

  @Test
  public void valid_get_operation_should_succeed() throws Exception {
    final UriInfo uri = Mockito.mock(UriInfo.class);
    when(uri.getQueryParameters()).thenReturn(requestParameters);
    requestParameters.addFirst("operation", "command");
    subject.acceptQuery(uri, async);
    assertThatResponseIsSuccessful();
  }

  @Test
  public void valid_form_operation_should_succeed() throws Exception {
    requestParameters.addFirst("operation", "command");
    subject.acceptForm(requestParameters, async);
    assertThatResponseIsSuccessful();
  }

  @Test
  public void valid_body_operation_should_succeed() throws Exception {
    subject.acceptBody("command", MediaType.valueOf("application/sql-operation"), async);
    assertThatResponseIsSuccessful();
  }

  private void assertThatResponseIsSuccessful() {
    assertThat(async.response(), hasStatus(Response.Status.OK));
    assertThat(async.response().getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
    assertThat(async.entity(byte[].class), is(payload));
  }

  // ===============================================================================================
  // INVOKING CONNECTOR

  private final ArgumentCaptor<Parameters> params = ArgumentCaptor.forClass(Parameters.class);

  @Test
  public void forward_all_query_parameters() throws Exception {
    final UriInfo uri = Mockito.mock(UriInfo.class);
    when(uri.getQueryParameters()).thenReturn(requestParameters);
    requestParameters.addAll("one", "1");
    requestParameters.addAll("two", "2", "3");
    subject.acceptQuery(uri, async);
    verify(connector).accept(params.capture());
    assertThat(params.getValue().properties(), hasEntries("one", "1"));
    assertThat(params.getValue().properties(), hasEntries("two", "2", "3"));
  }

  @Test
  public void forward_all_form_parameters() throws Exception {
    requestParameters.addAll("one", "1");
    requestParameters.addAll("two", "2", "3");
    subject.acceptForm(requestParameters, async);
    verify(connector).accept(params.capture());
    assertThat(params.getValue().properties(), hasEntries("one", "1"));
    assertThat(params.getValue().properties(), hasEntries("two", "2", "3"));
  }

  @Test
  public void forward_body_parameter() throws Exception {
    subject.acceptBody("1", MediaType.valueOf("application/sql-one"), async);
    verify(connector).accept(params.capture());
    assertThat(params.getValue().properties(), hasEntries("one", "1"));
  }

  // ===============================================================================================
  // INTERNAL ERRORS

  @Test
  public void send_error_if_connector_fails_fatally() throws Exception {
    final Throwable failure = new DatasetFailureException(new IllegalStateException());
    when(connector.accept(any(Parameters.class))).thenThrow(failure);
    try {
      subject.acceptBody("command", Mime.QUERY_SQL.type(), async);
    } catch (Exception ignored) {
    }
    assertThat(async.error(), is(failure));
  }

  @Test
  public void send_error_if_observable_fails() throws Exception {
    final Throwable failure = new DatasetFailureException(new IllegalStateException());
    when(connector.accept(any(Parameters.class))).thenReturn(Observable.<StreamedResults>error(failure));
    subject.acceptBody("command", Mime.QUERY_SQL.type(), async);
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

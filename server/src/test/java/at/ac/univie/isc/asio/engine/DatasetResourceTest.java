/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.brood.StubContainer;
import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.jaxrs.AsyncResponseFake;
import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import at.ac.univie.isc.asio.security.DelegatedCredentialsDetails;
import at.ac.univie.isc.asio.security.Identity;
import at.ac.univie.isc.asio.tool.Timeout;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import rx.Observable;

import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasHeader;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static at.ac.univie.isc.asio.junit.IsMultimapContaining.hasEntries;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatasetResourceTest {
  private final Connector connector = Mockito.mock(Connector.class);
  private final SecurityContextImpl securityContext = new SecurityContextImpl();
  private final StubContainer dataset = StubContainer.create("test");
  private final DatasetResource subject = new DatasetResource(dataset, connector, securityContext, Timeout.undefined());

  private final HttpHeaders headers = Mockito.mock(HttpHeaders.class);

  private final AsyncResponseFake async = AsyncResponseFake.create();
  private final MultivaluedMap<String, String> requestParameters = new MultivaluedHashMap<>();
  private final byte[] payload = Payload.randomWithLength(100);

  private DatasetResource.Params request = new DatasetResource.Params();

  @Before
  public void setUp() throws Exception {
    when(connector.accept(any(Command.class))).thenReturn(streamedResultsFrom(payload));
    request.language = Language.valueOf("test");
    request.headers = headers;
  }

  // ===============================================================================================
  // METADATA

  @Test
  public void should_respond_with_metadata_from_dataset() throws Exception {
    final SchemaDescriptor descriptor = SchemaDescriptor.empty("test").build();
    dataset.withMetadata(Observable.just(descriptor));
    final Response response = subject.fetchMetadata();
    assertThat(response, hasStatus(Response.Status.OK));
    assertThat(response.getEntity(), Matchers.<Object>equalTo(descriptor));
  }

  @Test
  public void should_respond_with_NOT_FOUND_if_dataset_has_no_metadata() throws Exception {
    assertThat(subject.fetchMetadata(), hasStatus(Response.Status.NOT_FOUND));
  }

  @Test
  public void should_respond_with_definition_from_dataset() throws Exception {
    final SqlSchema definition = new SqlSchema();
    dataset.withDefinition(Observable.just(definition));
    final Response response = subject.fetchDefinition();
    assertThat(response, hasStatus(Response.Status.OK));
    assertThat(response.getEntity(), Matchers.<Object>equalTo(definition));
  }

  @Test
  public void should_respond_with_NOT_FOUND_if_dataset_has_no_definition() throws Exception {
    assertThat(subject.fetchDefinition(), hasStatus(Response.Status.NOT_FOUND));
  }

  @Test
  public void should_redirect_to_new_schema_path() throws Exception {
    final UriInfo uri = Mockito.mock(UriInfo.class);
    when(uri.getAbsolutePath()).thenReturn(URI.create("http://localhost:8080/asio/public/read/meta/schema"));
    assertThat(subject.redirectFromDeprecatedDefinitionUri(uri), hasHeader(HttpHeaders.LOCATION, equalTo("http://localhost:8080/asio/public/read/schema")));
    when(uri.getAbsolutePath()).thenReturn(URI.create("http://localhost:8080/asio/meta/read/meta/schema"));
    assertThat(subject.redirectFromDeprecatedDefinitionUri(uri), hasHeader(HttpHeaders.LOCATION, equalTo("http://localhost:8080/asio/meta/read/schema")));
    when(uri.getAbsolutePath()).thenReturn(URI.create("http://localhost:8080/asio/public/read/meta/schema/"));
    assertThat(subject.redirectFromDeprecatedDefinitionUri(uri), hasHeader(HttpHeaders.LOCATION, equalTo("http://localhost:8080/asio/public/read/schema")));
  }

  @Test
  public void should_respond_with_mapping_model() throws Exception {
    final Model mappingModel = ModelFactory.createDefaultModel();
    dataset.withMapping(Observable.just(mappingModel));
    final Response response = subject.fetchMapping();
    assertThat(response, hasStatus(Response.Status.OK));
    assertThat(response.getEntity(), Matchers.<Object>equalTo(mappingModel));
  }

  @Test
  public void should_respond_with_NOT_FOUND_if_dataset_has_no_mapping() throws Exception {
    assertThat(subject.fetchMapping(), hasStatus(Response.Status.NOT_FOUND));
  }

  @Test
  public void should_send__OK__on_root_path() throws Exception {
    assertThat(subject.info(), hasStatus(Response.Status.OK));
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
    final List<MediaType> expected = Collections.singletonList(MediaType.APPLICATION_JSON_TYPE);
    when(headers.getAcceptableMediaTypes()).thenReturn(expected);
    subject.acceptForm(requestParameters, async, request);
    verify(connector).accept(params.capture());
    assertThat(params.getValue().acceptable(), is(expected));
  }

  @Test
  public void forward_request_principal() throws Exception {
    final TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "password");
    authentication.setDetails(new DelegatedCredentialsDetails(Identity.from("test", "password")));
    securityContext.setAuthentication(authentication);
    subject.acceptForm(requestParameters, async, request);
    verify(connector).accept(params.capture());
    assertThat(params.getValue().owner().get(), Matchers.<Principal>is(Identity.from("test", "password")));
  }

  @Test
  public void use_undefined_principal_if_missing() throws Exception {
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
    final Throwable failure = new IllegalStateException();
    when(connector.accept(any(Command.class))).thenThrow(failure);
    try {
      subject.acceptBody("command", Mime.QUERY_SQL.type(), async, request);
    } catch (Exception ignored) {
    }
    assertThat(async.error(), is(failure));
  }

  @Test
  public void send_error_if_observable_fails() throws Exception {
    final Throwable failure = new IllegalStateException();
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
